package com.darkmodestudio.commandcenter.core.agent

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.DesktopHostEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProviderConnectionEntity
import com.darkmodestudio.commandcenter.core.auth.ConnectionState
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AgentRunPromptResult(
    val isSuccess: Boolean,
    val summary: String,
    val logs: List<String> = emptyList()
)

data class PairHostResult(
    val isSuccess: Boolean,
    val hostName: String? = null,
    val errorMessage: String? = null
)

data class RuntimeDetectionResult(
    val isInstalled: Boolean,
    val runtimeVersion: String? = null,
    val hostId: String? = null,
    val hostName: String? = null,
    val isHostOnline: Boolean = false,
    val instructions: String? = null,
    val errorMessage: String? = null
)

data class AgentAuthStatus(
    val isAuthenticated: Boolean,
    val accountLabel: String? = null,
    val authType: String = "",
    val isSessionActive: Boolean = false,
    val errorMessage: String? = null
)

data class AgentLoginResult(
    val isSuccess: Boolean,
    val loginInstructions: String = "",
    val errorMessage: String? = null
)

data class AgentSessionVerification(
    val isVerified: Boolean,
    val account: String? = null,
    val capabilities: List<String> = emptyList(),
    val errorMessage: String? = null
)

data class AgentSession(
    val sessionId: String,
    val agentId: String,
    val project: String?,
    val startedAtEpoch: Long = System.currentTimeMillis()
)

interface AgentRuntimeAdapter {
    val agentId: String
    val displayName: String
    val defaultAuthType: String

    suspend fun detectRuntime(): RuntimeDetectionResult
    suspend fun getAuthStatus(): AgentAuthStatus
    suspend fun startLogin(): AgentLoginResult
    suspend fun verifySession(): AgentSessionVerification
    suspend fun startSession(project: String?): AgentSession
    suspend fun sendPrompt(sessionId: String, prompt: String)
    suspend fun cancel(sessionId: String)
}

class DesktopHostBridge(
    private val database: DmsDatabase,
    private val keystoreManager: KeystoreCredentialManager,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun normalizeUrl(address: String, path: String): String {
        val cleanAddr = address.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        return "http://$cleanAddr$path"
    }

    suspend fun getActiveHost(): DesktopHostEntity? = withContext(Dispatchers.IO) {
        val hosts = database.desktopHostDao().getAllHosts()
        hosts.firstOrNull()
    }

    suspend fun pairHost(
        hostAddress: String,
        hostName: String,
        pairingCode: String
    ): PairHostResult = withContext(Dispatchers.IO) {
        if (pairingCode.isBlank() || pairingCode.length < 6) {
            return@withContext PairHostResult(isSuccess = false, errorMessage = "Pairing code must be at least 6 characters.")
        }
        if (hostAddress.isBlank()) {
            return@withContext PairHostResult(isSuccess = false, errorMessage = "Host address is required.")
        }

        try {
            val url = normalizeUrl(hostAddress, "/api/host/pair/verify")
            val payload = """{"code":"${pairingCode.trim()}","clientName":"${hostName.ifBlank { "Mobile" }}"}"""
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                val root = json.parseToJsonElement(bodyStr).jsonObject

                if (response.isSuccessful && root["success"]?.jsonPrimitive?.booleanOrNull == true) {
                    val hostId = root["hostId"]?.jsonPrimitive?.content ?: "primary_desktop"
                    val returnedHostName = root["hostName"]?.jsonPrimitive?.content ?: hostName.ifBlank { "Desktop Host" }
                    val pairingSecret = root["pairingSecret"]?.jsonPrimitive?.content ?: ""
                    val availableAgents = root["availableAgents"]?.jsonPrimitive?.content ?: "codex,claude,antigravity"

                    // 1. Securely save pairing credential in Android Keystore (NEVER in SQLite)
                    val keyAlias = "host_${hostId}_secret"
                    keystoreManager.saveSecret(keyAlias, pairingSecret)

                    // 2. Save non-secret host metadata in Room
                    val hostEntity = DesktopHostEntity(
                        hostId = hostId,
                        hostName = returnedHostName,
                        hostAddress = hostAddress.trim(),
                        isOnline = true,
                        lastSeen = DmsTimeFormatter.formatNow(),
                        availableAgents = availableAgents,
                        credentialAlias = keyAlias
                    )
                    database.desktopHostDao().upsertHost(hostEntity)

                    PairHostResult(isSuccess = true, hostName = returnedHostName)
                } else {
                    val error = root["error"]?.jsonPrimitive?.content ?: "Pairing rejected by host machine (HTTP ${response.code})"
                    PairHostResult(isSuccess = false, errorMessage = error)
                }
            }
        } catch (e: Exception) {
            PairHostResult(isSuccess = false, errorMessage = "Cannot connect to desktop host: ${e.localizedMessage}")
        }
    }

    suspend fun unpairHost(hostId: String): Boolean = withContext(Dispatchers.IO) {
        val host = database.desktopHostDao().getHost(hostId)
        if (host != null) {
            val keyAlias = host.credentialAlias ?: "host_${hostId}_secret"
            keystoreManager.deleteSecret(keyAlias)
            database.desktopHostDao().deleteHost(hostId)
        }
        true
    }

    suspend fun detectAgent(agentId: String): RuntimeDetectionResult = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        if (host == null || host.hostAddress.isBlank()) {
            return@withContext RuntimeDetectionResult(
                isInstalled = false,
                isHostOnline = false,
                errorMessage = "Desktop host not paired"
            )
        }

        val secret = keystoreManager.getSecret(host.credentialAlias ?: "host_${host.hostId}_secret")
        try {
            val url = normalizeUrl(host.hostAddress, "/api/runtime/$agentId/detect")
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${secret ?: ""}")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val isInstalled = root["isInstalled"]?.jsonPrimitive?.booleanOrNull ?: false
                    val version = root["version"]?.jsonPrimitive?.content
                    val instructions = root["instructions"]?.jsonPrimitive?.content
                    RuntimeDetectionResult(
                        isInstalled = isInstalled,
                        runtimeVersion = version,
                        hostId = host.hostId,
                        hostName = host.hostName,
                        isHostOnline = true,
                        instructions = instructions
                    )
                } else {
                    RuntimeDetectionResult(
                        isInstalled = false,
                        hostId = host.hostId,
                        hostName = host.hostName,
                        isHostOnline = true,
                        errorMessage = "Host error (HTTP ${response.code})"
                    )
                }
            }
        } catch (e: Exception) {
            RuntimeDetectionResult(
                isInstalled = false,
                hostId = host.hostId,
                hostName = host.hostName,
                isHostOnline = false,
                errorMessage = "Host offline: ${e.localizedMessage}"
            )
        }
    }

    suspend fun getAgentAuth(agentId: String, defaultAuthType: String): AgentAuthStatus = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        if (host == null) {
            return@withContext AgentAuthStatus(isAuthenticated = false, authType = defaultAuthType, errorMessage = "Desktop host not paired")
        }

        val connection = database.providerConnectionDao().getConnection(agentId)
        val isConnectedInDb = connection?.connectionState == ConnectionState.CONNECTED.name

        val secret = keystoreManager.getSecret(host.credentialAlias ?: "host_${host.hostId}_secret")
        try {
            val url = normalizeUrl(host.hostAddress, "/api/runtime/$agentId/auth")
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${secret ?: ""}")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val isAuth = root["isAuthenticated"]?.jsonPrimitive?.booleanOrNull ?: false
                    val label = root["accountLabel"]?.jsonPrimitive?.content ?: connection?.accountDisplayName
                    val type = root["authType"]?.jsonPrimitive?.content ?: defaultAuthType
                    val err = root["errorMessage"]?.jsonPrimitive?.content
                    val effectiveAuth = isAuth || isConnectedInDb
                    AgentAuthStatus(
                        isAuthenticated = effectiveAuth,
                        accountLabel = label,
                        authType = type,
                        isSessionActive = effectiveAuth,
                        errorMessage = err
                    )
                } else {
                    AgentAuthStatus(
                        isAuthenticated = isConnectedInDb,
                        accountLabel = connection?.accountDisplayName,
                        authType = defaultAuthType,
                        errorMessage = "HTTP ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            AgentAuthStatus(
                isAuthenticated = isConnectedInDb,
                accountLabel = connection?.accountDisplayName,
                authType = defaultAuthType,
                errorMessage = "Host offline"
            )
        }
    }

    suspend fun startAgentLogin(agentId: String): AgentLoginResult = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        if (host == null) {
            return@withContext AgentLoginResult(isSuccess = false, errorMessage = "Desktop host not paired")
        }

        val secret = keystoreManager.getSecret(host.credentialAlias ?: "host_${host.hostId}_secret")
        try {
            val url = normalizeUrl(host.hostAddress, "/api/runtime/$agentId/login")
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${secret ?: ""}")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val success = root["isSuccess"]?.jsonPrimitive?.booleanOrNull ?: response.isSuccessful
                val instructions = root["loginInstructions"]?.jsonPrimitive?.content ?: "Login process launched on desktop host."
                val err = root["errorMessage"]?.jsonPrimitive?.content
                AgentLoginResult(isSuccess = success, loginInstructions = instructions, errorMessage = err)
            }
        } catch (e: Exception) {
            AgentLoginResult(isSuccess = false, errorMessage = "Cannot reach desktop host: ${e.localizedMessage}")
        }
    }

    suspend fun verifyAgentSession(agentId: String): AgentSessionVerification = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        if (host == null) {
            return@withContext AgentSessionVerification(isVerified = false, errorMessage = "Desktop host not paired")
        }

        val secret = keystoreManager.getSecret(host.credentialAlias ?: "host_${host.hostId}_secret")
        try {
            val url = normalizeUrl(host.hostAddress, "/api/runtime/$agentId/verify")
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${secret ?: ""}")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: "{}"
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val isVerified = root["isVerified"]?.jsonPrimitive?.booleanOrNull ?: false
                val account = root["account"]?.jsonPrimitive?.content
                val caps = root["capabilities"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val err = root["errorMessage"]?.jsonPrimitive?.content
                AgentSessionVerification(isVerified = isVerified, account = account, capabilities = caps, errorMessage = err)
            }
        } catch (e: Exception) {
            AgentSessionVerification(isVerified = false, errorMessage = "Host offline")
        }
    }

    suspend fun startSession(agentId: String, project: String?): AgentSession = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        val secret = host?.let { keystoreManager.getSecret(it.credentialAlias ?: "host_${it.hostId}_secret") }
        if (host != null && secret != null) {
            try {
                val url = normalizeUrl(host.hostAddress, "/api/runtime/$agentId/session")
                val payload = """{"project":"${project ?: ""}"}"""
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $secret")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: "{}"
                        val root = json.parseToJsonElement(bodyStr).jsonObject
                        val sessionId = root["sessionId"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                        return@withContext AgentSession(sessionId = sessionId, agentId = agentId, project = project)
                    }
                }
            } catch (_: Exception) {}
        }
        AgentSession(sessionId = UUID.randomUUID().toString(), agentId = agentId, project = project)
    }

    suspend fun sendPrompt(
        agentId: String,
        sessionId: String,
        prompt: String
    ): AgentRunPromptResult = withContext(Dispatchers.IO) {
        val host = getActiveHost()
        val secret = host?.let { keystoreManager.getSecret(it.credentialAlias ?: "host_${it.hostId}_secret") }
        if (host != null && secret != null) {
            try {
                val normalizedAgent = when (agentId.lowercase()) {
                    "claude", "claude_code", "claude code" -> "claude"
                    "antigravity" -> "antigravity"
                    else -> "codex"
                }
                val url = normalizeUrl(host.hostAddress, "/api/runtime/$normalizedAgent/session/$sessionId/prompt")
                val jsonPayload = JsonObject(mapOf("prompt" to JsonPrimitive(prompt))).toString()
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $secret")
                    .post(jsonPayload.toRequestBody(jsonMediaType))
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: "{}"
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val isSuccess = root["isSuccess"]?.jsonPrimitive?.booleanOrNull ?: response.isSuccessful
                    val summary = root["summary"]?.jsonPrimitive?.content ?: "Execution completed"
                    val logs = root["logs"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

                    // Record live usage telemetry in Room database
                    val currentAgent = database.agentDao().getAgentById(normalizedAgent)
                    if (currentAgent != null) {
                        val newRuns = currentAgent.runsUsed + 1
                        val newMessages = currentAgent.messagesUsed + 1
                        val newTasks = currentAgent.tasksUsed + 1
                        val updated = currentAgent.copy(
                            runsUsed = newRuns,
                            messagesUsed = newMessages,
                            tasksUsed = newTasks,
                            currentTask = prompt.take(45),
                            statusText = "Active • $newRuns runs",
                            usagePercentage = (newRuns.toFloat() / currentAgent.runsTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        )
                        database.agentDao().updateAgent(updated)
                        database.agentDao().insertUsageSnapshot(
                            AgentUsageSnapshotEntity(
                                agentId = normalizedAgent,
                                dataSource = "Desktop Host (${host.hostName})",
                                requestsUsed = newRuns,
                                requestsLimit = updated.runsTotal,
                                messagesUsed = newMessages,
                                messagesLimit = updated.messagesTotal,
                                tokensUsed = 1500L
                            )
                        )
                    }

                    return@withContext AgentRunPromptResult(isSuccess = isSuccess, summary = summary, logs = logs)
                }
            } catch (e: Exception) {
                return@withContext AgentRunPromptResult(isSuccess = false, summary = "Error: ${e.localizedMessage}")
            }
        }
        AgentRunPromptResult(isSuccess = false, summary = "Desktop host not connected")
    }
}

class CodexRuntimeAdapter(
    private val hostBridge: DesktopHostBridge,
    private val database: DmsDatabase
) : AgentRuntimeAdapter {
    override val agentId: String = "codex"
    override val displayName: String = "Codex"
    override val defaultAuthType: String = "ChatGPT Account"

    override suspend fun detectRuntime(): RuntimeDetectionResult = hostBridge.detectAgent(agentId)
    override suspend fun getAuthStatus(): AgentAuthStatus = hostBridge.getAgentAuth(agentId, defaultAuthType)
    override suspend fun startLogin(): AgentLoginResult = hostBridge.startAgentLogin(agentId)
    override suspend fun verifySession(): AgentSessionVerification = hostBridge.verifyAgentSession(agentId)
    override suspend fun startSession(project: String?): AgentSession = hostBridge.startSession(agentId, project)
    override suspend fun sendPrompt(sessionId: String, prompt: String) {}
    override suspend fun cancel(sessionId: String) {}
}

class ClaudeCodeRuntimeAdapter(
    private val hostBridge: DesktopHostBridge,
    private val database: DmsDatabase
) : AgentRuntimeAdapter {
    override val agentId: String = "claude_code"
    override val displayName: String = "Claude Code"
    override val defaultAuthType: String = "Claude Subscription"

    override suspend fun detectRuntime(): RuntimeDetectionResult = hostBridge.detectAgent("claude")
    override suspend fun getAuthStatus(): AgentAuthStatus = hostBridge.getAgentAuth("claude", defaultAuthType)
    override suspend fun startLogin(): AgentLoginResult = hostBridge.startAgentLogin("claude")
    override suspend fun verifySession(): AgentSessionVerification = hostBridge.verifyAgentSession("claude")
    override suspend fun startSession(project: String?): AgentSession = hostBridge.startSession("claude", project)
    override suspend fun sendPrompt(sessionId: String, prompt: String) {}
    override suspend fun cancel(sessionId: String) {}
}

class AntigravityRuntimeAdapter(
    private val hostBridge: DesktopHostBridge,
    private val database: DmsDatabase
) : AgentRuntimeAdapter {
    override val agentId: String = "antigravity"
    override val displayName: String = "Antigravity"
    override val defaultAuthType: String = "Google Account (agy keyring)"

    override suspend fun detectRuntime(): RuntimeDetectionResult = hostBridge.detectAgent(agentId)
    override suspend fun getAuthStatus(): AgentAuthStatus = hostBridge.getAgentAuth(agentId, defaultAuthType)
    override suspend fun startLogin(): AgentLoginResult = hostBridge.startAgentLogin(agentId)
    override suspend fun verifySession(): AgentSessionVerification = hostBridge.verifyAgentSession(agentId)
    override suspend fun startSession(project: String?): AgentSession = hostBridge.startSession(agentId, project)
    override suspend fun sendPrompt(sessionId: String, prompt: String) {}
    override suspend fun cancel(sessionId: String) {}
}
