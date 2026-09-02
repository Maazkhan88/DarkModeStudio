package com.darkmodestudio.commandcenter.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.sync.SyncCoordinator
import com.darkmodestudio.commandcenter.core.sync.SyncMode
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

sealed interface ConnectAuthState {
    data object Idle : ConnectAuthState
    data class Authorizing(val providerId: String) : ConnectAuthState
    data class ProcessingCallback(val providerId: String) : ConnectAuthState
    data class Connected(val providerId: String, val accountName: String) : ConnectAuthState
    data class Error(val providerId: String, val message: String) : ConnectAuthState
}

data class PendingOAuthTransaction(
    val providerId: String,
    val config: OAuthProviderConfig,
    val verifier: String,
    val state: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ConnectAuthCoordinator(
    private val database: DmsDatabase,
    private val keystoreManager: KeystoreCredentialManager,
    private val pkceManager: OAuthPkceManager = OAuthPkceManager(database, keystoreManager),
    private val syncCoordinator: SyncCoordinator? = null
) {
    private val _authState = MutableStateFlow<ConnectAuthState>(ConnectAuthState.Idle)
    val authState: StateFlow<ConnectAuthState> = _authState.asStateFlow()

    private val pendingTransactions = ConcurrentHashMap<String, PendingOAuthTransaction>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startOAuthFlow(context: Context, providerId: String): Boolean {
        val provider = ProviderRegistry.getProvider(providerId)
        if (provider == null) {
            _authState.value = ConnectAuthState.Error(providerId, "Provider $providerId is not recognized.")
            return false
        }

        val config = provider.oauthConfig
        if (config == null || !provider.isOAuthConfigured) {
            _authState.value = ConnectAuthState.Error(
                providerId,
                "OAuth 2.0 setup required for ${provider.displayName}. Please configure client ID or use Personal Access Token / API Key in Advanced mode."
            )
            return false
        }

        val state = pkceManager.generateState()
        val verifier = pkceManager.generateCodeVerifier()

        val pending = PendingOAuthTransaction(
            providerId = providerId,
            config = config,
            verifier = verifier,
            state = state
        )
        pendingTransactions[state] = pending

        val authUri = pkceManager.buildAuthorizationUri(config, state, verifier)
        _authState.value = ConnectAuthState.Authorizing(providerId)

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            pendingTransactions.remove(state)
            _authState.value = ConnectAuthState.Error(providerId, "Failed to launch browser: ${e.localizedMessage}")
            return false
        }
    }

    suspend fun handleDeepLink(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (uri.scheme != "darkmodestudio" || uri.host != "oauth" || uri.path != "/callback") {
            return@withContext false
        }

        val redirectResult = pkceManager.parseRedirectUrl(uri.toString())
        if (!redirectResult.isSuccess || redirectResult.code.isNullOrBlank()) {
            val errorMsg = redirectResult.errorDescription ?: redirectResult.error ?: "Authorization was cancelled or denied."
            _authState.value = ConnectAuthState.Error("oauth", errorMsg)
            return@withContext false
        }

        val returnedState = redirectResult.state
        if (returnedState.isNullOrBlank()) {
            _authState.value = ConnectAuthState.Error("oauth", "Callback missing state parameter.")
            return@withContext false
        }

        val pending = pendingTransactions.remove(returnedState)
        if (pending == null) {
            _authState.value = ConnectAuthState.Error("oauth", "Invalid or expired authorization transaction.")
            return@withContext false
        }

        // Validate state in constant time
        if (!pkceManager.validateState(pending.state, returnedState)) {
            _authState.value = ConnectAuthState.Error(pending.providerId, "OAuth state mismatch. Security verification failed.")
            return@withContext false
        }

        _authState.value = ConnectAuthState.ProcessingCallback(pending.providerId)

        // Token exchange
        val tokenResponse = pkceManager.exchangeCodeForToken(pending.config, redirectResult.code, pending.verifier)
        if (!tokenResponse.isSuccess || tokenResponse.accessToken.isNullOrBlank()) {
            val err = tokenResponse.errorMessage ?: "Failed to exchange authorization code for access token."
            _authState.value = ConnectAuthState.Error(pending.providerId, err)
            return@withContext false
        }

        // Account identity
        val account = pkceManager.fetchAccountIdentity(pending.providerId, tokenResponse.accessToken)

        // Save session
        val saved = pkceManager.saveAuthenticatedSession(
            providerId = pending.providerId,
            authMethod = AuthMethod.OAuthPkce,
            tokenResponse = tokenResponse,
            account = account
        )

        if (!saved) {
            _authState.value = ConnectAuthState.Error(pending.providerId, "Failed to securely save session credentials.")
            return@withContext false
        }

        // Update Integration state non-destructively
        val nowFormatted = DmsTimeFormatter.formatNow()
        val integration = IntegrationEntity(
            id = pending.providerId,
            name = ProviderRegistry.getProvider(pending.providerId)?.displayName ?: pending.providerId,
            category = ProviderRegistry.getProvider(pending.providerId)?.category?.displayName ?: "Integration",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = "Connected as ${account.displayName ?: "Authorized Account"}"
        )
        database.integrationDao().upsertIntegrationNonDestructively(integration)

        // Trigger sync coordinator
        syncCoordinator?.syncAll(SyncMode.MANUAL)

        val accountDisplayName = account.displayName ?: account.primaryEmail ?: "Authorized Account"
        _authState.value = ConnectAuthState.Connected(pending.providerId, accountDisplayName)
        true
    }

    suspend fun disconnect(providerId: String) = withContext(Dispatchers.IO) {
        pkceManager.disconnect(providerId)
        val existing = database.integrationDao().getIntegrationById(providerId)
        if (existing != null) {
            val updated = existing.copy(
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Disconnected",
                primaryMetric = "Disconnected — Tap to configure"
            )
            database.integrationDao().upsertIntegrationNonDestructively(updated)
        }
        syncCoordinator?.syncAll(SyncMode.MANUAL)
        _authState.value = ConnectAuthState.Idle
    }

    fun clearError() {
        _authState.value = ConnectAuthState.Idle
    }
}
