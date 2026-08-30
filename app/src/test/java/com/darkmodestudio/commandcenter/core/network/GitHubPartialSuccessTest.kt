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
class GitHubPartialSuccessTest {

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
    fun partialFailure_whenPullRequestsFail500_mapsToDegradedHealth() = runBlocking {
        val reposJson = """
            [
                {
                    "id": 201,
                    "name": "SecondMe",
                    "full_name": "Maazkhan88/SecondMe",
                    "private": false,
                    "description": "AI Memory Agent",
                    "created_at": "2026-08-30T10:00:00Z",
                    "pushed_at": "2026-08-30T12:00:00Z"
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                when {
                    url.contains("/user/repos") -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(reposJson.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    url.contains("/commits") -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    url.contains("/pulls") -> {
                        // PR request fails with 500 Internal Server Error
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(500)
                            .message("GitHub PR Internal Error")
                            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    url.contains("/actions/runs") -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{\"total_count\": 0, \"workflow_runs\": []}".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                }
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val syncer = GitHubSyncer(database, keystoreManager, connector)

        val result = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(result.isSuccess)
        assertTrue(result.message.contains("partial failures"))

        // Integration health must be DEGRADED (NOT Operational)
        val integration = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration)
        assertEquals(IntegrationHealth.DEGRADED, integration!!.health)
        assertTrue(integration.primaryMetric.contains("Partial Sync"))

        // Repository data must still be safely ingested
        assertEquals(1, database.projectDao().getProjectCount())
    }

    @Test
    fun partialFailure_whenWorkflowsFail500_mapsToDegradedHealth() = runBlocking {
        val reposJson = """
            [
                {
                    "id": 202,
                    "name": "GhostCart",
                    "full_name": "Maazkhan88/GhostCart",
                    "private": false,
                    "description": "Headless commerce",
                    "created_at": "2026-08-30T10:00:00Z",
                    "pushed_at": "2026-08-30T12:00:00Z"
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                when {
                    url.contains("/user/repos") -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(reposJson.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    url.contains("/actions/runs") -> {
                        // Actions request fails with 503 Service Unavailable
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(503)
                            .message("Actions Unavailable")
                            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                }
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val syncer = GitHubSyncer(database, keystoreManager, connector)

        val result = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(result.isSuccess)

        val integration = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration)
        assertEquals(IntegrationHealth.DEGRADED, integration!!.health)
    }
}
