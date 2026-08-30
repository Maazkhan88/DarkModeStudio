package com.darkmodestudio.commandcenter.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
