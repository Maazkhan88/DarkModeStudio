package com.darkmodestudio.commandcenter.core.auth

import android.net.Uri
import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
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
class ConnectAuthCoordinatorTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager
    private lateinit var pkceManager: OAuthPkceManager
    private lateinit var coordinator: ConnectAuthCoordinator

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()

                when {
                    url.contains("github.com/login/oauth/access_token") -> {
                        val body = """{
                            "access_token": "gho_mock_live_oauth_token",
                            "token_type": "bearer",
                            "scope": "repo,read:org,workflow,user:email"
                        }""".trimIndent()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    url.contains("api.github.com/user") -> {
                        val body = """{
                            "login": "Maazkhan88",
                            "id": 1234567,
                            "name": "Maaz Khan",
                            "email": "maaz@darkmodestudio.com"
                        }""".trimIndent()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                    else -> {
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                            .build()
                    }
                }
            })
            .build()

        pkceManager = OAuthPkceManager(database, keystoreManager, mockClient)
        coordinator = ConnectAuthCoordinator(database, keystoreManager, pkceManager)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun startOAuthFlow_withUnconfiguredProvider_emitsTruthfulError() {
        val context = RuntimeEnvironment.getApplication()
        val launched = coordinator.startOAuthFlow(context, "cloudflare")
        assertFalse("Unconfigured provider OAuth must not launch browser", launched)

        val state = coordinator.authState.value
        assertTrue(state is ConnectAuthState.Error)
        val err = state as ConnectAuthState.Error
        assertEquals("cloudflare", err.providerId)
        assertTrue(err.message.contains("setup required"))
    }

    @Test
    fun startOAuthFlow_withConfiguredGitHub_launchesFlowAndSetsAuthorizingState() {
        val context = RuntimeEnvironment.getApplication()
        val launched = coordinator.startOAuthFlow(context, "github")
        assertTrue(launched)

        val state = coordinator.authState.value
        assertTrue(state is ConnectAuthState.Authorizing)
        assertEquals("github", (state as ConnectAuthState.Authorizing).providerId)
    }

    @Test
    fun handleDeepLink_validCallback_exchangesCodeAndConnectsAccount() = runBlocking {
        val context = RuntimeEnvironment.getApplication()

        // Seed initial integration with metrics and incidents to verify non-destructive update
        database.integrationDao().upsertIntegrationNonDestructively(
            IntegrationEntity(
                id = "github",
                name = "GitHub",
                category = "Code & CI/CD",
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Never",
                primaryMetric = "Disconnected"
            )
        )
        database.integrationDao().insertMetrics(
            listOf(IntegrationMetricEntity(integrationId = "github", label = "CI Success Rate", value = "99.4%"))
        )
        database.integrationDao().insertIncidents(
            listOf(IntegrationIncidentEntity(id = "inc-1", integrationId = "github", title = "Action Runner Delay", description = "Resolved", timestamp = "Today", isResolved = true))
        )

        // 1. Start OAuth Flow
        coordinator.startOAuthFlow(context, "github")

        // Read the generated state from pending transaction
        val pendingMapField = ConnectAuthCoordinator::class.java.getDeclaredField("pendingTransactions")
        pendingMapField.isAccessible = true
        val pendingMap = pendingMapField.get(coordinator) as Map<*, *>
        val stateKey = pendingMap.keys.first() as String

        // 2. Simulate Callback Deep Link
        val callbackUri = Uri.parse("darkmodestudio://oauth/callback?code=gh_auth_code_9988&state=$stateKey")
        val handled = coordinator.handleDeepLink(callbackUri)
        assertTrue(handled)

        // 3. Verify Auth State
        val finalState = coordinator.authState.value
        assertTrue(finalState is ConnectAuthState.Connected)
        assertEquals("github", (finalState as ConnectAuthState.Connected).providerId)
        assertEquals("Maazkhan88", finalState.accountName)

        // 4. Verify Secret in Keystore ONLY
        assertEquals("gho_mock_live_oauth_token", keystoreManager.getSecret("oauth_github_access"))

        // 5. Verify Room Metadata
        val connection = database.providerConnectionDao().getConnection("github")
        assertNotNull(connection)
        assertEquals("CONNECTED", connection!!.connectionState)
        assertEquals("Maazkhan88", connection.accountDisplayName)

        // 6. Verify non-destructive integration update (Metrics and Incidents PRESERVED!)
        val integration = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration)
        assertTrue(integration!!.isConnected)
        assertEquals(IntegrationHealth.OPERATIONAL, integration.health)
        assertEquals(1, database.integrationDao().getMetricCount("github"))
        assertEquals(1, database.integrationDao().getIncidentCount("github"))
    }

    @Test
    fun handleDeepLink_invalidState_rejectedWithoutExchange() = runBlocking {
        val callbackUri = Uri.parse("darkmodestudio://oauth/callback?code=bad_code&state=non_existent_state")
        val handled = coordinator.handleDeepLink(callbackUri)
        assertFalse(handled)

        val state = coordinator.authState.value
        assertTrue(state is ConnectAuthState.Error)
        assertTrue((state as ConnectAuthState.Error).message.contains("Invalid or expired"))
    }

    @Test
    fun handleDeepLink_providerError_reportsError() = runBlocking {
        val callbackUri = Uri.parse("darkmodestudio://oauth/callback?error=user_cancelled&error_description=User+denied+access")
        val handled = coordinator.handleDeepLink(callbackUri)
        assertFalse(handled)

        val state = coordinator.authState.value
        assertTrue(state is ConnectAuthState.Error)
        assertTrue((state as ConnectAuthState.Error).message.contains("User denied access"))
    }

    @Test
    fun disconnect_clearsCredentialsAndMarksDisconnected() = runBlocking {
        keystoreManager.saveSecret("oauth_github_access", "mock_secret")
        database.integrationDao().upsertIntegrationNonDestructively(
            IntegrationEntity(
                id = "github",
                name = "GitHub",
                category = "Code & CI/CD",
                isConnected = true,
                health = IntegrationHealth.OPERATIONAL,
                lastSync = "Now",
                primaryMetric = "Connected"
            )
        )

        coordinator.disconnect("github")

        assertNull(keystoreManager.getSecret("oauth_github_access"))
        val integration = database.integrationDao().getIntegrationById("github")
        assertNotNull(integration)
        assertFalse(integration!!.isConnected)
        assertEquals(IntegrationHealth.DISCONNECTED, integration.health)
    }
}
