package com.darkmodestudio.commandcenter.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackConnectorsTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun testCloudflareZoneDtoParsing() {
        val jsonStr = """
            {
                "success": true,
                "result": [
                    {
                        "id": "zone_123",
                        "name": "darkmodestudio.dev",
                        "status": "active",
                        "paused": false,
                        "type": "full"
                    }
                ]
            }
        """.trimIndent()

        val parsed: CloudflareZonesResponseDto = json.decodeFromString(jsonStr)
        assertTrue(parsed.success)
        assertEquals(1, parsed.result.size)
        assertEquals("darkmodestudio.dev", parsed.result.first().name)
        assertEquals("active", parsed.result.first().status)
    }

    @Test
    fun testVercelDeploymentsParsing() {
        val jsonStr = """
            {
                "deployments": [
                    {
                        "uid": "dpl_456",
                        "name": "secondme-web",
                        "url": "secondme-web.app",
                        "state": "READY",
                        "created": 1725000000000
                    }
                ]
            }
        """.trimIndent()

        val parsed: VercelDeploymentsResponseDto = json.decodeFromString(jsonStr)
        assertEquals(1, parsed.deployments.size)
        assertEquals("secondme-web.app", parsed.deployments.first().url)
        assertEquals("READY", parsed.deployments.first().state)
    }

    @Test
    fun supabaseConnector_onNetworkError_returnsFailureWithoutFabricatedTelemetry() = runBlocking {
        val errorClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        val connector = SupabaseConnector(errorClient)
        val result = connector.pingHealth("https://mock.supabase.co", "test_key")

        assertFalse("HTTP 503 must report failure", result.isSuccess)
        assertNotNull("Error message must be present", result.errorMessage)
        assertNull("Pool usage must NOT be fabricated on error", result.poolUsagePercent)
        assertNull("Storage must NOT be fabricated on error", result.storageUsedGb)
    }

    @Test
    fun vercelConnector_onNetworkError_returnsFailureWithoutFabricatedTelemetry() = runBlocking {
        val errorClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{\"error\": \"Invalid token\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            })
            .build()

        val connector = VercelConnector(errorClient)
        val result = connector.fetchDeployments("invalid_token")

        assertFalse("HTTP 401 must report failure", result.isSuccess)
        assertNotNull("Error message must be present", result.errorMessage)
        assertNull("Production URL must NOT be fabricated on error", result.productionUrl)
        assertNull("Deployments count must NOT be fabricated on error", result.dailyDeployments)
    }
}
