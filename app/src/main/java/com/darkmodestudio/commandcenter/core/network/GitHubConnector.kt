package com.darkmodestudio.commandcenter.core.network

import com.darkmodestudio.commandcenter.core.network.model.GitHubCommitDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubPullDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubRateLimitResponseDto
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
        token: String,
        targetRepos: List<Pair<String, String>> = listOf("darkmodestudio" to "core")
    ): GitHubTelemetryResult = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch User Repositories
            val reposRequest = buildRequest("https://api.github.com/user/repos?per_page=15&sort=updated", token)
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
                } else if (response.code == 403 && rateLimitRemaining == 0) {
                    return@withContext GitHubTelemetryResult(
                        isSuccess = false,
                        rateLimitRemaining = 0,
                        rateLimitResetAt = rateLimitReset,
                        errorMessage = "GitHub API Rate limit exceeded. Resets at $rateLimitReset"
                    )
                }
            }

            val commitsMap = mutableMapOf<String, List<GitHubCommitDto>>()
            val pullsMap = mutableMapOf<String, List<GitHubPullDto>>()
            val workflowsMap = mutableMapOf<String, GitHubWorkflowRunsResponseDto>()

            // 2. Fetch Details for Target Repos
            for ((owner, repo) in targetRepos) {
                val repoKey = "$owner/$repo"

                // Fetch Commits
                try {
                    val commitsReq = buildRequest("https://api.github.com/repos/$owner/$repo/commits?per_page=5", token)
                    okHttpClient.newCall(commitsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.header("ETag")?.let { eTagCache["commits_$repoKey"] = it }
                            val body = resp.body?.string() ?: "[]"
                            commitsMap[repoKey] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {}

                // Fetch Pull Requests
                try {
                    val pullsReq = buildRequest("https://api.github.com/repos/$owner/$repo/pulls?state=open&per_page=10", token)
                    okHttpClient.newCall(pullsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.header("ETag")?.let { eTagCache["pulls_$repoKey"] = it }
                            val body = resp.body?.string() ?: "[]"
                            pullsMap[repoKey] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {}

                // Fetch Workflow Runs
                try {
                    val workflowsReq = buildRequest("https://api.github.com/repos/$owner/$repo/actions/runs?per_page=10", token)
                    okHttpClient.newCall(workflowsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.header("ETag")?.let { eTagCache["workflows_$repoKey"] = it }
                            val body = resp.body?.string() ?: "{}"
                            workflowsMap[repoKey] = json.decodeFromString(body)
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

    private fun buildRequest(url: String, token: String, cacheKey: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DarkModeStudio-CommandCenter")

        if (cacheKey != null && eTagCache.containsKey(cacheKey)) {
            builder.header("If-None-Match", eTagCache[cacheKey]!!)
        }

        return builder.build()
    }
}
