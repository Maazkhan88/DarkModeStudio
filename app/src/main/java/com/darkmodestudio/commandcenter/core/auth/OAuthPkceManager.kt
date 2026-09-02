package com.darkmodestudio.commandcenter.core.auth

import android.net.Uri
import android.util.Base64
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.ProviderConnectionEntity
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

data class OAuthProviderConfig(
    val providerId: String,
    val clientId: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val revokeEndpoint: String? = null,
    val redirectUri: String = "darkmodestudio://oauth/callback",
    val scopes: List<String> = emptyList(),
    val supportsPkce: Boolean = true
)

data class OAuthRedirectResult(
    val isSuccess: Boolean,
    val code: String? = null,
    val state: String? = null,
    val error: String? = null,
    val errorDescription: String? = null
)

data class OAuthTokenResponse(
    val isSuccess: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresInSeconds: Long? = null,
    val scope: String? = null,
    val tokenType: String? = null,
    val errorMessage: String? = null
)

data class ProviderAccountIdentity(
    val accountId: String? = null,
    val displayName: String? = null,
    val workspaceName: String? = null,
    val primaryEmail: String? = null
)

class OAuthPkceManager(
    private val database: DmsDatabase,
    private val keystoreManager: KeystoreCredentialManager,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val secureRandom = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP).trim()
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP).trim()
    }

    fun generateState(): String = UUID.randomUUID().toString()

    fun buildAuthorizationUri(config: OAuthProviderConfig, state: String, codeVerifier: String): String {
        val challenge = generateCodeChallenge(codeVerifier)
        val uriBuilder = Uri.parse(config.authorizationEndpoint).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", config.clientId)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("state", state)

        if (config.scopes.isNotEmpty()) {
            uriBuilder.appendQueryParameter("scope", config.scopes.joinToString(" "))
        }

        if (config.supportsPkce) {
            uriBuilder.appendQueryParameter("code_challenge", challenge)
            uriBuilder.appendQueryParameter("code_challenge_method", "S256")
        }

        return uriBuilder.build().toString()
    }

    fun parseRedirectUrl(redirectUrl: String): OAuthRedirectResult {
        return try {
            val uri = Uri.parse(redirectUrl)
            val error = uri.getQueryParameter("error")
            val errorDescription = uri.getQueryParameter("error_description")

            if (!error.isNullOrBlank()) {
                return OAuthRedirectResult(
                    isSuccess = false,
                    error = error,
                    errorDescription = errorDescription
                )
            }

            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")

            if (code.isNullOrBlank()) {
                OAuthRedirectResult(isSuccess = false, error = "missing_code", errorDescription = "No authorization code returned")
            } else {
                OAuthRedirectResult(isSuccess = true, code = code, state = state)
            }
        } catch (e: Exception) {
            OAuthRedirectResult(isSuccess = false, error = "parse_error", errorDescription = e.localizedMessage)
        }
    }

    fun validateState(expectedState: String, returnedState: String?): Boolean {
        if (returnedState.isNullOrBlank()) return false
        return MessageDigest.isEqual(
            expectedState.toByteArray(StandardCharsets.UTF_8),
            returnedState.toByteArray(StandardCharsets.UTF_8)
        )
    }

    suspend fun exchangeCodeForToken(
        config: OAuthProviderConfig,
        code: String,
        codeVerifier: String
    ): OAuthTokenResponse = withContext(Dispatchers.IO) {
        try {
            val formBuilder = FormBody.Builder()
                .add("client_id", config.clientId)
                .add("code", code)
                .add("redirect_uri", config.redirectUri)
                .add("grant_type", "authorization_code")

            if (config.supportsPkce) {
                formBuilder.add("code_verifier", codeVerifier)
            }

            val request = Request.Builder()
                .url(config.tokenEndpoint)
                .header("Accept", "application/json")
                .header("User-Agent", "DarkModeStudio-CommandCenter")
                .post(formBuilder.build())
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val accessToken = root["access_token"]?.jsonPrimitive?.content
                    val refreshToken = root["refresh_token"]?.jsonPrimitive?.content
                    val error = root["error"]?.jsonPrimitive?.content
                    val errorDesc = root["error_description"]?.jsonPrimitive?.content

                    if (!error.isNullOrBlank() || accessToken.isNullOrBlank()) {
                        OAuthTokenResponse(
                            isSuccess = false,
                            errorMessage = errorDesc ?: error ?: "Failed to receive access token"
                        )
                    } else {
                        val expiresIn = root["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
                        val scope = root["scope"]?.jsonPrimitive?.content
                        val tokenType = root["token_type"]?.jsonPrimitive?.content
                        OAuthTokenResponse(
                            isSuccess = true,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresInSeconds = expiresIn,
                            scope = scope,
                            tokenType = tokenType
                        )
                    }
                } else {
                    OAuthTokenResponse(
                        isSuccess = false,
                        errorMessage = "HTTP ${response.code}: ${response.message.ifBlank { "Token endpoint error" }}"
                    )
                }
            }
        } catch (e: Exception) {
            OAuthTokenResponse(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Token exchange network error"
            )
        }
    }

    suspend fun fetchAccountIdentity(
        providerId: String,
        accessToken: String
    ): ProviderAccountIdentity = withContext(Dispatchers.IO) {
        try {
            if (providerId.equals("github", ignoreCase = true)) {
                val request = Request.Builder()
                    .url("https://api.github.com/user")
                    .header("Authorization", "Bearer $accessToken")
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "DarkModeStudio-CommandCenter")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: "{}"
                        val root = json.parseToJsonElement(body).jsonObject
                        val login = root["login"]?.jsonPrimitive?.content
                        val name = root["name"]?.jsonPrimitive?.content
                        val id = root["id"]?.jsonPrimitive?.content
                        val email = root["email"]?.jsonPrimitive?.content
                        return@withContext ProviderAccountIdentity(
                            accountId = id,
                            displayName = login ?: name ?: "GitHub User",
                            workspaceName = login,
                            primaryEmail = email
                        )
                    }
                }
            }
            ProviderAccountIdentity(
                accountId = "acct_${System.currentTimeMillis()}",
                displayName = "Authorized $providerId Account"
            )
        } catch (e: Exception) {
            ProviderAccountIdentity(
                accountId = "acct_unknown",
                displayName = "Authorized $providerId Account"
            )
        }
    }

    suspend fun saveAuthenticatedSession(
        providerId: String,
        authMethod: AuthMethod,
        tokenResponse: OAuthTokenResponse,
        account: ProviderAccountIdentity
    ): Boolean = withContext(Dispatchers.IO) {
        if (!tokenResponse.isSuccess || tokenResponse.accessToken.isNullOrBlank()) {
            return@withContext false
        }

        // 1. Securely encrypt tokens in Keystore (Secrets are NEVER saved in Room)
        keystoreManager.saveSecret("oauth_${providerId}_access", tokenResponse.accessToken)
        if (!tokenResponse.refreshToken.isNullOrBlank()) {
            keystoreManager.saveSecret("oauth_${providerId}_refresh", tokenResponse.refreshToken)
        }

        // 2. Persist non-secret connection metadata in Room SQLite
        val expiresAtEpoch = tokenResponse.expiresInSeconds?.let { System.currentTimeMillis() + (it * 1000) }
        val connectionEntity = ProviderConnectionEntity(
            providerId = providerId,
            authMethod = authMethod.displayName,
            connectionState = ConnectionState.CONNECTED.name,
            accountDisplayName = account.displayName ?: account.primaryEmail ?: "Authorized Account",
            accountId = account.accountId,
            workspaceName = account.workspaceName,
            grantedScopes = tokenResponse.scope,
            expiresAt = expiresAtEpoch,
            lastVerifiedAt = DmsTimeFormatter.formatNow(),
            lastError = null
        )

        database.providerConnectionDao().upsertConnection(connectionEntity)
        true
    }

    suspend fun disconnect(providerId: String): Boolean = withContext(Dispatchers.IO) {
        // 1. Remove encrypted secrets from Keystore
        keystoreManager.deleteSecret("oauth_${providerId}_access")
        keystoreManager.deleteSecret("oauth_${providerId}_refresh")
        keystoreManager.deleteSecret("token_$providerId")

        // 2. Update connection state in Room
        val existing = database.providerConnectionDao().getConnection(providerId)
        val updated = ProviderConnectionEntity(
            providerId = providerId,
            authMethod = existing?.authMethod ?: AuthMethod.OAuthPkce.displayName,
            connectionState = ConnectionState.DISCONNECTED.name,
            accountDisplayName = null,
            accountId = null,
            workspaceName = null,
            grantedScopes = null,
            expiresAt = null,
            lastVerifiedAt = DmsTimeFormatter.formatNow(),
            lastError = null
        )
        database.providerConnectionDao().upsertConnection(updated)
        true
    }
}
