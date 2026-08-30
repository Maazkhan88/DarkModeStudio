package com.darkmodestudio.commandcenter.core.network

import com.darkmodestudio.commandcenter.core.network.model.GitHubCommitDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubPullDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubRepoDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubWorkflowRunsResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class GitHubTelemetryResult(
    val isSuccess: Boolean,
    val isNotModified: Boolean = false,
    val repos: List<GitHubRepoDto> = emptyList(),
    val commitsByRepo: Map<String, List<GitHubCommitDto>> = emptyMap(),
    val pullsByRepo: Map<String, List<GitHubPullDto>> = emptyMap(),
    val workflowsByRepo: Map<String, GitHubWorkflowRunsResponseDto> = emptyMap(),
    val rateLimitRemaining: Int = 5000,
    val rateLimitResetAt: Long = 0,
    val errorMessage: String? = null
)

class GitHubConnector(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    ) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val eTagCache = ConcurrentHashMap<String, String>()

    suspend fun fetchAllTelemetry(
        token: String
    ): GitHubTelemetryResult = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch User Repositories (Up to 30 most recently pushed)
            val reposRequest = buildRequest("https://api.github.com/user/repos?per_page=30&sort=pushed", token)
            var repos: List<GitHubRepoDto> = emptyList()
            var isNotModified = false
            var rateLimitRemaining = 5000
            var rateLimitReset: Long = 0

            okHttpClient.newCall(reposRequest).execute().use { response ->
                rateLimitRemaining = response.header("x-ratelimit-remaining")?.toIntOrNull() ?: 5000
                rateLimitReset = response.header("x-ratelimit-reset")?.toLongOrNull() ?: 0

                if (response.code == 304) {
                    isNotModified = true
                } else if (response.isSuccessful) {
                    response.header("ETag")?.let { eTagCache["repos"] = it }
                    val body = response.body?.string() ?: "[]"
                    repos = json.decodeFromString(body)
                } else if (response.code == 401) {
                    return@withContext GitHubTelemetryResult(
                        isSuccess = false,
                        errorMessage = "Invalid or expired GitHub Personal Access Token (401 Unauthorized)"
                    )
                }
            }

            val commitsMap = mutableMapOf<String, List<GitHubCommitDto>>()
            val pullsMap = mutableMapOf<String, List<GitHubPullDto>>()
            val workflowsMap = mutableMapOf<String, GitHubWorkflowRunsResponseDto>()

            // 2. Fetch Details for All Returned User Repositories
            for (repo in repos.take(10)) {
                val fullName = repo.fullName

                // Fetch Real Commits
                try {
                    val commitsReq = buildRequest("https://api.github.com/repos/$fullName/commits?per_page=10", token)
                    okHttpClient.newCall(commitsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            commitsMap[fullName] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {}

                // Fetch Real Pull Requests
                try {
                    val pullsReq = buildRequest("https://api.github.com/repos/$fullName/pulls?state=open&per_page=10", token)
                    okHttpClient.newCall(pullsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            pullsMap[fullName] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {}

                // Fetch Real Actions Workflow Runs
                try {
                    val workflowsReq = buildRequest("https://api.github.com/repos/$fullName/actions/runs?per_page=10", token)
                    okHttpClient.newCall(workflowsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "{}"
                            workflowsMap[fullName] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {}
            }

            GitHubTelemetryResult(
                isSuccess = true,
                isNotModified = isNotModified,
                repos = repos,
                commitsByRepo = commitsMap,
                pullsByRepo = pullsMap,
                workflowsByRepo = workflowsMap,
                rateLimitRemaining = rateLimitRemaining,
                rateLimitResetAt = rateLimitReset
            )
        } catch (e: Exception) {
            GitHubTelemetryResult(
                isSuccess = false,
                errorMessage = e.message ?: "GitHub network connection error"
            )
        }
    }

    private fun buildRequest(url: String, token: String): Request {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DarkModeStudio-CommandCenter")
            .build()
    }
}
