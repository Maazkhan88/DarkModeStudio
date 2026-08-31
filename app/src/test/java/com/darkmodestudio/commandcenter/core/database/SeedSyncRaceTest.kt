package com.darkmodestudio.commandcenter.core.database

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.sync.GitHubSyncer
import com.darkmodestudio.commandcenter.core.sync.SyncCoordinator
import com.darkmodestudio.commandcenter.core.sync.SyncMode
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SeedSyncRaceTest {

    private lateinit var database: DmsDatabase
    private lateinit var initializer: AppDataInitializer
    private lateinit var keystoreManager: KeystoreCredentialManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        initializer = AppDataInitializer(database)
        keystoreManager = KeystoreCredentialManager(context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun initializer_createsOnlyStructuralDefaults_zeroFakeProjects() = runBlocking {
        initializer.initialize()

        // 1. App settings must exist
        val settings = database.settingsDao().getSettings()
        assertNotNull(settings)
        assertEquals(6, settings!!.schemaSeedVersion)

        // 2. Zero fake projects created
        val projectCount = database.projectDao().getProjectCount()
        assertEquals(0, projectCount)

        // 3. Default disconnected integrations created
        val githubIntegration = database.integrationDao().getIntegrationById("github")
        assertNotNull(githubIntegration)
        assertEquals(IntegrationHealth.DISCONNECTED, githubIntegration!!.health)

        // 4. Default structural agents created
        val codex = database.agentDao().getAgentById("codex")
        assertNotNull(codex)
        assertEquals(0, codex!!.runsUsed)
        assertEquals(500, codex.runsTotal)
    }

    @Test
    fun initializer_preservesUserAgentTelemetry_acrossMultipleInvocationsAndSyncs() = runBlocking {
        // First run initializes agents
        initializer.initialize()

        val codexInitial = database.agentDao().getAgentById("codex")
        assertNotNull(codexInitial)

        // Simulate agent telemetry progression during active use
        val updatedCodex = codexInitial!!.copy(
            runsUsed = 85,
            messagesUsed = 2100,
            tasksUsed = 14,
            currentTask = "Synthesizing AST for multi-agent bridge",
            statusText = "Analyzing Codebase • 42%",
            usagePercentage = 0.42f
        )
        database.agentDao().updateAgent(updatedCodex)

        // Re-run AppDataInitializer (e.g. app restart)
        initializer.initialize()

        // Verify that custom agent state was NOT reset to 0
        val codexAfterInit = database.agentDao().getAgentById("codex")
        assertNotNull(codexAfterInit)
        assertEquals(85, codexAfterInit!!.runsUsed)
        assertEquals(2100, codexAfterInit.messagesUsed)
        assertEquals(14, codexAfterInit.tasksUsed)
        assertEquals("Synthesizing AST for multi-agent bridge", codexAfterInit.currentTask)
        assertEquals("Analyzing Codebase • 42%", codexAfterInit.statusText)

        // Execute full sync coordinator syncAll
        val coordinator = SyncCoordinator(database, keystoreManager)
        coordinator.syncAll(SyncMode.FOREGROUND)

        // Verify that syncAll also NEVER wipes out agent telemetry
        val codexAfterSync = database.agentDao().getAgentById("codex")
        assertNotNull(codexAfterSync)
        assertEquals(85, codexAfterSync!!.runsUsed)
        assertEquals(2100, codexAfterSync.messagesUsed)
        assertEquals(14, codexAfterSync.tasksUsed)
        assertEquals("Synthesizing AST for multi-agent bridge", codexAfterSync.currentTask)
    }

    @Test
    fun initializer_preservesCustomizedAutomationRules() = runBlocking {
        initializer.initialize()

        val rule1 = database.automationDao().getRuleById("rule1")
        assertNotNull(rule1)
        assertTrue(rule1!!.isEnabled)

        // User customizes rule1: disables it and modifies notification copy
        val modifiedRule = rule1.copy(
            isEnabled = false,
            humanReadableText = "Custom User Workflow Alert: Silence CI alerts on weekends"
        )
        database.automationDao().updateRule(modifiedRule)

        // Re-run initializer
        initializer.initialize()

        val rule1After = database.automationDao().getRuleById("rule1")
        assertNotNull(rule1After)
        assertFalse(rule1After!!.isEnabled)
        assertEquals("Custom User Workflow Alert: Silence CI alerts on weekends", rule1After.humanReadableText)
    }

    @Test
    fun startupSequence_liveSyncedDataNeverOverwritten() = runBlocking {
        // Step 1: Initializer runs
        initializer.initialize()

        // Step 2: Live GitHub sync runs with mock response
        val mockReposJson = """
            [
                {
                    "id": 999,
                    "name": "LiveProductionRepo",
                    "full_name": "Maazkhan88/LiveProductionRepo",
                    "private": false,
                    "description": "Real live project from GitHub",
                    "default_branch": "develop",
                    "created_at": "2026-08-30T10:00:00Z",
                    "pushed_at": "2026-08-30T12:00:00Z"
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(mockReposJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        keystoreManager.saveSecret("token_github", "test_mock_token")
        val connector = GitHubConnector(mockClient)
        val syncer = GitHubSyncer(database, keystoreManager, connector)

        val syncResult = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(syncResult.isSuccess)

        // Verify project is in database
        val projectsCount = database.projectDao().getProjectCount()
        assertEquals(1, projectsCount)
        val project = database.projectDao().getProjectById("liveproductionrepo")
        assertNotNull(project)
        assertEquals("LiveProductionRepo", project!!.name)
        assertEquals("Maazkhan88/LiveProductionRepo", project.repositoryFullName)
        assertEquals("develop", project.repositoryDefaultBranch)

        // Step 3: Re-running initializer on subsequent app launch must NOT wipe or add fake projects
        initializer.initialize()
        assertEquals(1, database.projectDao().getProjectCount())
    }
}
