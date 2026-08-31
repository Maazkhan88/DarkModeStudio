package com.darkmodestudio.commandcenter.core.agent

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.DesktopHostEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProviderConnectionEntity
import com.darkmodestudio.commandcenter.core.auth.ConnectionState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        hostBridge = DesktopHostBridge(database)
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
        assertEquals("DMS Desktop Host not paired or offline", codexDetection.errorMessage)

        val claudeDetection = claudeAdapter.detectRuntime()
        assertFalse(claudeDetection.isInstalled)
        assertFalse(claudeDetection.isHostOnline)

        val agyDetection = antigravityAdapter.detectRuntime()
        assertFalse(agyDetection.isInstalled)
        assertFalse(agyDetection.isHostOnline)

        // Login attempt fails with truthful explanation
        val loginResult = codexAdapter.startLogin()
        assertFalse(loginResult.isSuccess)
        assertTrue(loginResult.errorMessage!!.contains("offline"))
    }

    @Test
    fun agents_whenDesktopHostPaired_detectAvailableRuntimes() = runBlocking {
        val paired = hostBridge.pairHost(
            hostId = "primary_desktop",
            hostName = "Maaz-MacBook",
            pairingCode = "DMS-994821"
        )
        assertTrue(paired)

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
        hostBridge.pairHost("primary_desktop", "Dev-PC", "DMS-123456")

        // Unauthenticated initial state
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
        assertEquals("ChatGPT Plus Session", authStatus.accountLabel)
        assertEquals("ChatGPT Account", authStatus.authType)

        val session = codexAdapter.startSession("secondme")
        assertNotNull(session)
        assertEquals("codex", session.agentId)
        assertEquals("secondme", session.project)
    }

    @Test
    fun hostBridge_unpairHost_removesHostRecordAndResetsAgentsToOffline() = runBlocking {
        hostBridge.pairHost("primary_desktop", "Workstation", "DMS-789012")
        assertTrue(codexAdapter.detectRuntime().isHostOnline)

        val unpaired = hostBridge.unpairHost("primary_desktop")
        assertTrue(unpaired)

        assertFalse(codexAdapter.detectRuntime().isHostOnline)
        assertFalse(claudeAdapter.detectRuntime().isHostOnline)
        assertFalse(antigravityAdapter.detectRuntime().isHostOnline)
    }
}
