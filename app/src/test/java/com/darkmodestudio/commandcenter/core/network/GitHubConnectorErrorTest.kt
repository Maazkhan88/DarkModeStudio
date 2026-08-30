package com.darkmodestudio.commandcenter.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GitHubConnectorErrorTest {

    private fun createMockClient(statusCode: Int, responseBody: String, headers: Map<String, String> = emptyMap()): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val builder = Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(statusCode)
                .message("Mock Status $statusCode")
                .body(responseBody.toResponseBody("application/json".toMediaType()))

            headers.forEach { (k, v) -> builder.header(k, v) }
            builder.build()
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    private fun createFailingClient(exception: IOException): OkHttpClient {
        val interceptor = Interceptor { _ ->
            throw exception
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Test
    fun testEmptyTokenReturnsNoCredentials() = runBlocking {
        val connector = GitHubConnector(OkHttpClient())
        val result = connector.fetchAllTelemetry("")

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.NO_CREDENTIALS, result.status)
    }

    @Test
    fun testNullTokenReturnsNoCredentials() = runBlocking {
        val connector = GitHubConnector(OkHttpClient())
        val result = connector.fetchAllTelemetry(null)

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.NO_CREDENTIALS, result.status)
    }

    @Test
    fun testAuthFailure401HandledExplicitly() = runBlocking {
        val mockClient = createMockClient(401, "{\"message\":\"Bad credentials\"}")
        val connector = GitHubConnector(mockClient)
        val result = connector.fetchAllTelemetry("invalid_token")

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.AUTH_FAILURE, result.status)
        assertTrue(result.errorMessage?.contains("401") == true)
    }

    @Test
    fun testRateLimited403HandledExplicitly() = runBlocking {
        val mockClient = createMockClient(
            statusCode = 403,
            responseBody = "{\"message\":\"API rate limit exceeded\"}",
            headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1788100800")
        )
        val connector = GitHubConnector(mockClient)
        val result = connector.fetchAllTelemetry("valid_token")

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.RATE_LIMITED, result.status)
    }

    @Test
    fun testServerFailure500HandledExplicitly() = runBlocking {
        val mockClient = createMockClient(500, "{\"message\":\"Internal Server Error\"}")
        val connector = GitHubConnector(mockClient)
        val result = connector.fetchAllTelemetry("valid_token")

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.SERVER_FAILURE, result.status)
    }

    @Test
    fun testNetworkExceptionHandledExplicitly() = runBlocking {
        val failingClient = createFailingClient(IOException("Failed to connect to host"))
        val connector = GitHubConnector(failingClient)
        val result = connector.fetchAllTelemetry("valid_token")

        assertFalse(result.isSuccess)
        assertEquals(GitHubSyncStatus.NETWORK_FAILURE, result.status)
        assertTrue(result.errorMessage?.contains("Failed to connect") == true)
    }

    @Test
    fun testSuccessfulEmptyRepositories() = runBlocking {
        val mockClient = createMockClient(200, "[]")
        val connector = GitHubConnector(mockClient)
        val result = connector.fetchAllTelemetry("valid_token")

        assertTrue(result.isSuccess)
        assertEquals(GitHubSyncStatus.SUCCESS, result.status)
        assertEquals(0, result.repos.size)
    }
}
