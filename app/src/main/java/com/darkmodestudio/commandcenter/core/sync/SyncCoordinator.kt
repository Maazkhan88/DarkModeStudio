package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.network.CloudflareConnector
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.network.SupabaseConnector
import com.darkmodestudio.commandcenter.core.network.VercelConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class SyncMode {
    FOREGROUND,
    MANUAL,
    BACKGROUND
}

data class ProviderSyncResult(
    val provider: SecureProvider,
    val isSuccess: Boolean,
    val message: String
)

interface ProviderSyncer {
    val provider: SecureProvider
    suspend fun sync(mode: SyncMode): ProviderSyncResult
}

class SyncCoordinator(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val gitHubConnector: GitHubConnector = GitHubConnector(),
    private val cloudflareConnector: CloudflareConnector = CloudflareConnector(),
    private val supabaseConnector: SupabaseConnector = SupabaseConnector(),
    private val vercelConnector: VercelConnector = VercelConnector()
) {

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val automationEvaluator = AutomationEvaluator(database)

    private val syncers: List<ProviderSyncer> = listOf(
        GitHubSyncer(database, keystoreCredentialManager, gitHubConnector),
        CloudflareSyncer(database, keystoreCredentialManager, cloudflareConnector),
        SupabaseSyncer(database, keystoreCredentialManager, supabaseConnector),
        VercelSyncer(database, keystoreCredentialManager, vercelConnector)
    )

    suspend fun syncAll(mode: SyncMode): List<ProviderSyncResult> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING)
        val results = mutableListOf<ProviderSyncResult>()

        for (syncer in syncers) {
            try {
                val result = syncer.sync(mode)
                results.add(result)
            } catch (e: Exception) {
                results.add(
                    ProviderSyncResult(
                        provider = syncer.provider,
                        isSuccess = false,
                        message = e.message ?: "Sync error"
                    )
                )
            }
        }

        // Evaluate event-driven automation rules following telemetry ingestion
        try {
            automationEvaluator.evaluateAllRules()
        } catch (_: Exception) {}

        val allSuccess = results.all { it.isSuccess }
        _syncState.value = SyncState(
            status = if (allSuccess) SyncStatus.SUCCESS else SyncStatus.ERROR,
            lastSyncTimestamp = System.currentTimeMillis(),
            providerResults = results
        )

        results
    }
}
