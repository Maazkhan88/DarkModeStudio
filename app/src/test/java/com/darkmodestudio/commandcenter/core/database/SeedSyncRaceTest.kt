package com.darkmodestudio.commandcenter.core.database

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.network.GitHubSyncStatus
import com.darkmodestudio.commandcenter.core.network.GitHubTelemetryResult
import com.darkmodestudio.commandcenter.core.network.model.GitHubRepoDto
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.sync.GitHubSyncer
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
        assertEquals(4, settings!!.schemaSeedVersion)

        // 2. Zero fake projects created
        val projectCount = database.projectDao().getProjectCount()
        assertEquals(0, projectCount)

        // 3. Default disconnected integrations created
        val githubIntegration = database.integrationDao().getIntegrationById("github")
        assertNotNull(githubIntegration)
        assertEquals(IntegrationHealth.DISCONNECTED, githubIntegration!!.health)
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

        // Step 3: Re-running initializer on subsequent app launch must NOT wipe or add fake projects
        initializer.initialize()
        assertEquals(1, database.projectDao().getProjectCount())
    }
}
