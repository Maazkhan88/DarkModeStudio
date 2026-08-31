package com.darkmodestudio.commandcenter.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class SupabaseTelemetryResult(
    val isSuccess: Boolean,
    val latencyMs: Long? = null,
    val poolUsagePercent: Int? = null,
    val storageUsedGb: Float? = null,
    val storageTotalGb: Float? = null,
    val isDegraded: Boolean = false,
    val alertMessage: String? = null,
    val errorMessage: String? = null
)

class SupabaseConnector(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    suspend fun pingHealth(supabaseUrl: String, apiKey: String): SupabaseTelemetryResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val url = if (supabaseUrl.endsWith("/")) "${supabaseUrl}rest/v1/" else "$supabaseUrl/rest/v1/"
            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "DarkModeStudio-CommandCenter")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (response.isSuccessful || response.code == 404) {
                    SupabaseTelemetryResult(
                        isSuccess = true,
                        latencyMs = duration,
                        isDegraded = false,
                        alertMessage = null
                    )
                } else {
                    SupabaseTelemetryResult(
                        isSuccess = false,
                        latencyMs = duration,
                        errorMessage = "HTTP ${response.code}: ${response.message.ifBlank { "Service error" }}"
                    )
                }
            }
        } catch (e: Exception) {
            SupabaseTelemetryResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Network connection failed"
            )
        }
    }
}

@Serializable
data class VercelDeploymentDto(
    val uid: String = "",
    val name: String = "",
    val url: String = "",
    val state: String = "",
    val created: Long = 0
)

@Serializable
data class VercelDeploymentsResponseDto(
    val deployments: List<VercelDeploymentDto> = emptyList()
)

data class VercelTelemetryResult(
    val isSuccess: Boolean,
    val productionUrl: String? = null,
    val buildStatus: String? = null,
    val dailyDeployments: Int? = null,
    val edgeLatencyMs: Int? = null,
    val errorMessage: String? = null
)

class VercelConnector(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun fetchDeployments(token: String): VercelTelemetryResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.vercel.com/v6/deployments?limit=5")
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "DarkModeStudio-CommandCenter")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val parsed = json.decodeFromString<VercelDeploymentsResponseDto>(body)
                    val latest = parsed.deployments.firstOrNull()

                    val statusStr = when (latest?.state) {
                        "READY" -> "Ready"
                        "BUILDING" -> "Building"
                        "ERROR" -> "Failed"
                        "CANCELED" -> "Canceled"
                        else -> latest?.state ?: "Unknown"
                    }

                    VercelTelemetryResult(
                        isSuccess = true,
                        productionUrl = latest?.url,
                        buildStatus = statusStr,
                        dailyDeployments = parsed.deployments.size,
                        edgeLatencyMs = null
                    )
                } else {
                    VercelTelemetryResult(
                        isSuccess = false,
                        errorMessage = "HTTP ${response.code}: ${response.message.ifBlank { "Vercel API error" }}"
                    )
                }
            }
        } catch (e: Exception) {
            VercelTelemetryResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Vercel network connection error"
            )
        }
    }
}
