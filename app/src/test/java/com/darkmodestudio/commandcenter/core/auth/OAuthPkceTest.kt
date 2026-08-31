package com.darkmodestudio.commandcenter.core.auth

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.runBlocking
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
class OAuthPkceTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager
    private lateinit var pkceManager: OAuthPkceManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)
        pkceManager = OAuthPkceManager(database, keystoreManager)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun pkce_verifierAndChallenge_generatedWithProperEntropyAndEncoding() {
        val verifier1 = pkceManager.generateCodeVerifier()
        val verifier2 = pkceManager.generateCodeVerifier()

        assertTrue("Verifier must be at least 43 chars", verifier1.length >= 43)
        assertTrue("Verifier must be at least 43 chars", verifier2.length >= 43)
        assertFalse("Verifiers must be cryptographically random and unique", verifier1 == verifier2)

        val challenge = pkceManager.generateCodeChallenge(verifier1)
        assertTrue("Challenge must not be blank", challenge.isNotBlank())
        assertFalse("Challenge must not contain padding", challenge.contains("="))
        assertFalse("Challenge must not contain newlines", challenge.contains("\n"))
    }

    @Test
    fun pkce_authorizationUriBuilder_containsAllRequiredParameters() {
        val config = OAuthProviderConfig(
            providerId = "github",
            clientId = "dms_test_client_id",
            authorizationEndpoint = "https://github.com/login/oauth/authorize",
            tokenEndpoint = "https://github.com/login/oauth/access_token",
            redirectUri = "darkmodestudio://oauth/callback",
            scopes = listOf("read:user", "repo"),
            supportsPkce = true
        )

        val state = pkceManager.generateState()
        val verifier = pkceManager.generateCodeVerifier()
        val uri = pkceManager.buildAuthorizationUri(config, state, verifier)

        assertTrue(uri.contains("client_id=dms_test_client_id"))
        assertTrue(uri.contains("response_type=code"))
        assertTrue(uri.contains("redirect_uri=darkmodestudio%3A%2F%2Foauth%2Fcallback"))
        assertTrue(uri.contains("state=$state"))
        assertTrue(uri.contains("code_challenge_method=S256"))
        assertTrue(uri.contains("scope=read%3Auser%20repo"))
    }

    @Test
    fun pkce_stateValidation_acceptsMatchingAndRejectsMismatchedOrNull() {
        val expectedState = pkceManager.generateState()

        assertTrue("Matching state must be accepted", pkceManager.validateState(expectedState, expectedState))
        assertFalse("Mismatched state must be rejected", pkceManager.validateState(expectedState, "different_random_state"))
        assertFalse("Null state must be rejected", pkceManager.validateState(expectedState, null))
        assertFalse("Blank state must be rejected", pkceManager.validateState(expectedState, ""))
    }

    @Test
    fun pkce_redirectUrlParsing_extractsCodeAndDetectsErrors() {
        val successUrl = "darkmodestudio://oauth/callback?code=gh_auth_code_12345&state=state_abc"
        val successResult = pkceManager.parseRedirectUrl(successUrl)
        assertTrue(successResult.isSuccess)
        assertEquals("gh_auth_code_12345", successResult.code)
        assertEquals("state_abc", successResult.state)

        val errorUrl = "darkmodestudio://oauth/callback?error=access_denied&error_description=User+cancelled+authorization"
        val errorResult = pkceManager.parseRedirectUrl(errorUrl)
        assertFalse(errorResult.isSuccess)
        assertEquals("access_denied", errorResult.error)
        assertEquals("User cancelled authorization", errorResult.errorDescription)
    }

    @Test
    fun pkce_saveSessionAndDisconnect_persistsMetadataInRoomAndSecretsInKeystore() = runBlocking {
        val tokenResponse = OAuthTokenResponse(
            isSuccess = true,
            accessToken = "gho_mock_access_token_secret",
            refreshToken = "ghr_mock_refresh_token_secret",
            expiresInSeconds = 28800,
            scope = "repo read:user"
        )
        val identity = ProviderAccountIdentity(
            accountId = "usr_101",
            displayName = "Octocat Dev",
            workspaceName = "OctoOrg",
            primaryEmail = "octocat@github.com"
        )

        val saved = pkceManager.saveAuthenticatedSession("github", AuthMethod.OAuthPkce, tokenResponse, identity)
        assertTrue(saved)

        // Secrets in Keystore
        assertEquals("gho_mock_access_token_secret", keystoreManager.getSecret("oauth_github_access"))
        assertEquals("ghr_mock_refresh_token_secret", keystoreManager.getSecret("oauth_github_refresh"))

        // Non-secret metadata in Room
        val connection = database.providerConnectionDao().getConnection("github")
        assertNotNull(connection)
        assertEquals("CONNECTED", connection!!.connectionState)
        assertEquals("Octocat Dev", connection.accountDisplayName)
        assertEquals("OctoOrg", connection.workspaceName)

        // Disconnect
        val disconnected = pkceManager.disconnect("github")
        assertTrue(disconnected)

        // Secrets removed
        assertNull(keystoreManager.getSecret("oauth_github_access"))
        assertNull(keystoreManager.getSecret("oauth_github_refresh"))

        // Connection marked DISCONNECTED in Room
        val postDisconnect = database.providerConnectionDao().getConnection("github")
        assertNotNull(postDisconnect)
        assertEquals("DISCONNECTED", postDisconnect!!.connectionState)
        assertNull(postDisconnect.accountDisplayName)
    }
}
