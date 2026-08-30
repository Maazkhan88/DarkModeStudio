package com.darkmodestudio.commandcenter.core.network

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
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
class GitHub304Test {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)
        keystoreManager.saveSecret("token_github", "test_mock_token")
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun eTag304_preservesCachedProjectsAndDoesNotZeroOutTelemetry() = runBlocking {
        val mockReposJson = """
            [
                {
                    "id": 101,
                    "name": "DarkModeStudio",
                    "full_name": "Maazkhan88/DarkModeStudio",
                    "private": false,
                    "description": "AI Agent Command Center",
                    "created_at": "2026-08-30T10:00:00Z",
                    "pushed_at": "2026-08-30T12:00:00Z"
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val ifNoneMatch = request.header("If-None-Match")

                if (url.contains("/user/repos")) {
                    if (!ifNoneMatch.isNullOrBlank()) {
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(304)
                            .message("Not Modified")
                            .body("".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    } else {
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("ETag", "W/\"mock-etag-12345\"")
                            .body(mockReposJson.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                } else if (url.contains("/commits")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("[]".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                } else if (url.contains("/pulls")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("[]".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                } else if (url.contains("/actions/runs")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{\"total_count\": 0, \"workflow_runs\": []}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                } else {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val syncer = GitHubSyncer(database, keystoreManager, connector)

        // 1. First sync -> Populates Room with 1 project
        val result1 = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(result1.isSuccess)
        assertEquals(1, database.projectDao().getProjectCount())

        val initialProject = database.projectDao().getProjectById("darkmodestudio")
        assertNotNull(initialProject)
        assertEquals("DarkModeStudio", initialProject!!.name)

        val integration1 = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration1)
        assertEquals(IntegrationHealth.OPERATIONAL, integration1!!.health)

        // 2. Second sync -> Connector sends If-None-Match header and receives 304 Not Modified
        val result2 = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(result2.isSuccess)
        assertEquals("GitHub telemetry unchanged (304 Not Modified)", result2.message)

        // 3. Assertions: Room project count must still be 1 (NOT zeroed out!)
        assertEquals(1, database.projectDao().getProjectCount())
        val preservedProject = database.projectDao().getProjectById("darkmodestudio")
        assertNotNull(preservedProject)
        assertEquals("DarkModeStudio", preservedProject!!.name)

        // 4. Integration health must remain operational and not show 0 repos
        val integration2 = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration2)
        assertEquals(IntegrationHealth.OPERATIONAL, integration2!!.health)
    }
}
