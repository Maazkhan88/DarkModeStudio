package com.darkmodestudio.commandcenter.core.agent

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.DesktopHostEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProviderConnectionEntity
import com.darkmodestudio.commandcenter.core.auth.ConnectionState
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AgentRuntimeAdaptersTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager
    private lateinit var hostBridge: DesktopHostBridge
    private lateinit var codexAdapter: CodexRuntimeAdapter
    private lateinit var claudeAdapter: ClaudeCodeRuntimeAdapter
    private lateinit var antigravityAdapter: AntigravityRuntimeAdapter

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath

                when {
                    path.contains("/api/host/pair/verify") -> {
                        val body = """{
                            "success": true,
                            "hostId": "primary_desktop",
                            "hostName": "Maaz-MacBook",
                            "pairingSecret": "dms_host_sec_mock1234567890",
                            "availableAgents": "codex,claude,antigravity"
                        }""".trimIndent()
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    path.contains("/api/runtime/codex/detect") -> {
                        val body = """{
                            "isInstalled": true,
                            "version": "Codex CLI v0.9.4",
                            "isAuthenticated": true
                        }""".trimIndent()
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    path.contains("/api/runtime/claude/detect") -> {
                        val body = """{
                            "isInstalled": true,
                            "version": "claude-code v1.0.8",
                            "isAuthenticated": true
                        }""".trimIndent()
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    path.contains("/api/runtime/antigravity/detect") -> {
                        val body = """{
                            "isInstalled": true,
                            "version": "Antigravity 2.0 (CLI)",
                            "isAuthenticated": true
                        }""".trimIndent()
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("""{"success": true}""".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                }
            })
            .build()

        hostBridge = DesktopHostBridge(database, keystoreManager, mockClient)
        codexAdapter = CodexRuntimeAdapter(hostBridge, database)
        claudeAdapter = ClaudeCodeRuntimeAdapter(hostBridge, database)
        antigravityAdapter = AntigravityRuntimeAdapter(hostBridge, database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun agents_whenNoDesktopHostPaired_reportOfflineAndMissingRuntime() = runBlocking {
        val codexDetection = codexAdapter.detectRuntime()
        assertFalse(codexDetection.isInstalled)
        assertFalse(codexDetection.isHostOnline)
        assertEquals("Desktop host not paired", codexDetection.errorMessage)

        val claudeDetection = claudeAdapter.detectRuntime()
        assertFalse(claudeDetection.isInstalled)
        assertFalse(claudeDetection.isHostOnline)

        val agyDetection = antigravityAdapter.detectRuntime()
        assertFalse(agyDetection.isInstalled)
        assertFalse(agyDetection.isHostOnline)

        val loginResult = codexAdapter.startLogin()
        assertFalse(loginResult.isSuccess)
        assertTrue(loginResult.errorMessage!!.contains("Desktop host not paired"))
    }

    @Test
    fun agents_whenDesktopHostPaired_storesSecretOnlyInKeystoreAndDetectsRuntimes() = runBlocking {
        val pairResult = hostBridge.pairHost(
            hostAddress = "192.168.1.50:8998",
            hostName = "Maaz-MacBook",
            pairingCode = "DMS-994821"
        )
        assertTrue(pairResult.isSuccess)
        assertEquals("Maaz-MacBook", pairResult.hostName)

        // Secret is stored in Keystore
        assertEquals("dms_host_sec_mock1234567890", keystoreManager.getSecret("host_primary_desktop_secret"))

        // Host in Room contains NO secret
        val hostEntity = database.desktopHostDao().getHost("primary_desktop")
        assertNotNull(hostEntity)
        assertEquals("host_primary_desktop_secret", hostEntity!!.credentialAlias)
        assertEquals("192.168.1.50:8998", hostEntity.hostAddress)

        val codexDetection = codexAdapter.detectRuntime()
        assertTrue(codexDetection.isInstalled)
        assertTrue(codexDetection.isHostOnline)
        assertEquals("Codex CLI v0.9.4", codexDetection.runtimeVersion)
        assertEquals("primary_desktop", codexDetection.hostId)

        val claudeDetection = claudeAdapter.detectRuntime()
        assertTrue(claudeDetection.isInstalled)
        assertEquals("claude-code v1.0.8", claudeDetection.runtimeVersion)

        val agyDetection = antigravityAdapter.detectRuntime()
        assertTrue(agyDetection.isInstalled)
        assertEquals("Antigravity 2.0 (CLI)", agyDetection.runtimeVersion)
    }

    @Test
    fun agents_authStatus_reflectsTruthfulConnectionState() = runBlocking {
        hostBridge.pairHost("192.168.1.50:8998", "Dev-PC", "DMS-123456")

        val unauthStatus = codexAdapter.getAuthStatus()
        assertFalse("Must not be authenticated before login", unauthStatus.isAuthenticated)

        // Simulate successful login / session verification
        database.providerConnectionDao().upsertConnection(
            ProviderConnectionEntity(
                providerId = "codex",
                authMethod = "ChatGPT Account",
                connectionState = ConnectionState.CONNECTED.name,
                accountDisplayName = "ChatGPT Plus Session",
                lastVerifiedAt = "Today"
            )
        )

        val authStatus = codexAdapter.getAuthStatus()
        assertTrue(authStatus.isAuthenticated)

        val session = codexAdapter.startSession("secondme")
        assertNotNull(session)
        assertEquals("codex", session.agentId)
        assertEquals("secondme", session.project)
    }

    @Test
    fun hostBridge_unpairHost_removesHostRecordAndResetsAgentsToOffline() = runBlocking {
        hostBridge.pairHost("192.168.1.50:8998", "Workstation", "DMS-789012")
        assertTrue(codexAdapter.detectRuntime().isHostOnline)

        val unpaired = hostBridge.unpairHost("primary_desktop")
        assertTrue(unpaired)

        // Keystore secret deleted
        assertNull(keystoreManager.getSecret("host_primary_desktop_secret"))

        assertFalse(codexAdapter.detectRuntime().isHostOnline)
        assertFalse(claudeAdapter.detectRuntime().isHostOnline)
        assertFalse(antigravityAdapter.detectRuntime().isHostOnline)
    }
}
