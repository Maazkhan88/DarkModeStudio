package com.darkmodestudio.commandcenter.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubRepoSummary(
    val name: String,
    val fullName: String,
    val defaultBranch: String,
    val openIssuesCount: Int,
    val pushedAt: String
)

data class GitHubWorkflowSummary(
    val totalRuns: Int,
    val passingRuns: Int,
    val failingRuns: Int,
    val latestStatus: String
)

data class GitHubSyncResult(
    val isSuccess: Boolean,
    val repos: List<GitHubRepoSummary> = emptyList(),
    val workflows: GitHubWorkflowSummary? = null,
    val rateLimitRemaining: Int = 5000,
    val errorMessage: String? = null
)

class GitHubConnector(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchRepoTelemetry(token: String, owner: String, repo: String): GitHubSyncResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/actions/runs?per_page=10")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "DarkModeStudio-CommandCenter")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext GitHubSyncResult(
                        isSuccess = false,
                        errorMessage = "HTTP ${response.code}: ${response.message}"
                    )
                }

                val body = response.body?.string() ?: ""
                val root = json.parseToJsonElement(body).jsonObject
                val runs = root["workflow_runs"]?.jsonArray ?: JsonArray(emptyList())

                var passing = 0
                var failing = 0
                runs.forEach { run ->
                    val conclusion = run.jsonObject["conclusion"]?.jsonPrimitive?.content
                    if (conclusion == "success") passing++
                    else if (conclusion == "failure") failing++
                }

                val rateLimit = response.header("x-ratelimit-remaining")?.toIntOrNull() ?: 4900

                GitHubSyncResult(
                    isSuccess = true,
                    workflows = GitHubWorkflowSummary(
                        totalRuns = runs.size,
                        passingRuns = passing,
                        failingRuns = failing,
                        latestStatus = if (failing == 0) "Passing" else "Degraded"
                    ),
                    rateLimitRemaining = rateLimit
                )
            }
        } catch (e: Exception) {
            GitHubSyncResult(
                isSuccess = false,
                errorMessage = e.message ?: "Network connection error"
            )
        }
    }
}
