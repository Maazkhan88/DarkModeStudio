package com.darkmodestudio.commandcenter.core.network

import com.darkmodestudio.commandcenter.core.network.model.GitHubCommitDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubContentDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubPullDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubRepoDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubWorkflowRunsResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class GitHubSyncStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    AUTH_FAILURE,
    RATE_LIMITED,
    NETWORK_FAILURE,
    SERVER_FAILURE,
    NO_CREDENTIALS
}

data class GitHubTelemetryResult(
    val status: GitHubSyncStatus,
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
        token: String?
    ): GitHubTelemetryResult = withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) {
            return@withContext GitHubTelemetryResult(
                status = GitHubSyncStatus.NO_CREDENTIALS,
                isSuccess = false,
                errorMessage = "GitHub Personal Access Token not configured"
            )
        }

        try {
            // 1. Fetch User Repositories (Up to 30 most recently pushed)
            val reposEtag = eTagCache["repos"]
            val reposRequest = buildRequest("https://api.github.com/user/repos?per_page=30&sort=pushed", token, reposEtag)
            var repos: List<GitHubRepoDto> = emptyList()
            var isNotModified = false
            var rateLimitRemaining = 5000
            var rateLimitReset: Long = 0

            okHttpClient.newCall(reposRequest).execute().use { response ->
                rateLimitRemaining = response.header("x-ratelimit-remaining")?.toIntOrNull() ?: 5000
                rateLimitReset = response.header("x-ratelimit-reset")?.toLongOrNull() ?: 0

                when {
                    response.code == 304 -> {
                        isNotModified = true
                    }
                    response.code == 401 -> {
                        return@withContext GitHubTelemetryResult(
                            status = GitHubSyncStatus.AUTH_FAILURE,
                            isSuccess = false,
                            errorMessage = "Invalid or expired GitHub Personal Access Token (401 Unauthorized)"
                        )
                    }
                    response.code == 403 || rateLimitRemaining == 0 -> {
                        return@withContext GitHubTelemetryResult(
                            status = GitHubSyncStatus.RATE_LIMITED,
                            isSuccess = false,
                            errorMessage = "GitHub API rate limit exceeded (403 Forbidden)"
                        )
                    }
                    response.code >= 500 -> {
                        return@withContext GitHubTelemetryResult(
                            status = GitHubSyncStatus.SERVER_FAILURE,
                            isSuccess = false,
                            errorMessage = "GitHub Server Error (${response.code})"
                        )
                    }
                    !response.isSuccessful -> {
                        return@withContext GitHubTelemetryResult(
                            status = GitHubSyncStatus.NETWORK_FAILURE,
                            isSuccess = false,
                            errorMessage = "GitHub API error: ${response.code} ${response.message}"
                        )
                    }
                    else -> {
                        response.header("ETag")?.let { eTagCache["repos"] = it }
                        val body = response.body?.string() ?: "[]"
                        repos = json.decodeFromString(body)
                    }
                }
            }

            val commitsMap = mutableMapOf<String, List<GitHubCommitDto>>()
            val pullsMap = mutableMapOf<String, List<GitHubPullDto>>()
            val workflowsMap = mutableMapOf<String, GitHubWorkflowRunsResponseDto>()
            var hadPartialFailures = false

            // 2. Fetch Details for Returned User Repositories
            for (repo in repos.take(10)) {
                val fullName = repo.fullName

                // Fetch Commits
                try {
                    val commitsReq = buildRequest("https://api.github.com/repos/$fullName/commits?per_page=10", token)
                    okHttpClient.newCall(commitsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            commitsMap[fullName] = json.decodeFromString(body)
                        } else {
                            hadPartialFailures = true
                        }
                    }
                } catch (_: Exception) {
                    hadPartialFailures = true
                }

                // Fetch Pull Requests
                try {
                    val pullsReq = buildRequest("https://api.github.com/repos/$fullName/pulls?state=open&per_page=10", token)
                    okHttpClient.newCall(pullsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            pullsMap[fullName] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {
                    hadPartialFailures = true
                }

                // Fetch Actions Workflow Runs
                try {
                    val workflowsReq = buildRequest("https://api.github.com/repos/$fullName/actions/runs?per_page=10", token)
                    okHttpClient.newCall(workflowsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "{}"
                            workflowsMap[fullName] = json.decodeFromString(body)
                        }
                    }
                } catch (_: Exception) {
                    hadPartialFailures = true
                }
            }

            GitHubTelemetryResult(
                status = if (hadPartialFailures) GitHubSyncStatus.PARTIAL_SUCCESS else GitHubSyncStatus.SUCCESS,
                isSuccess = true,
                isNotModified = isNotModified,
                repos = repos,
                commitsByRepo = commitsMap,
                pullsByRepo = pullsMap,
                workflowsByRepo = workflowsMap,
                rateLimitRemaining = rateLimitRemaining,
                rateLimitResetAt = rateLimitReset
            )
        } catch (e: IOException) {
            GitHubTelemetryResult(
                status = GitHubSyncStatus.NETWORK_FAILURE,
                isSuccess = false,
                errorMessage = e.message ?: "Network failure connecting to GitHub API"
            )
        } catch (e: Exception) {
            GitHubTelemetryResult(
                status = GitHubSyncStatus.NETWORK_FAILURE,
                isSuccess = false,
                errorMessage = e.message ?: "GitHub connection error"
            )
        }
    }

    suspend fun fetchRepoContents(
        token: String?,
        fullName: String,
        path: String = ""
    ): List<GitHubContentDto> = withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) return@withContext emptyList()
        val url = if (path.isBlank()) {
            "https://api.github.com/repos/$fullName/contents"
        } else {
            "https://api.github.com/repos/$fullName/contents/$path"
        }

        try {
            val request = buildRequest(url, token)
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    json.decodeFromString(body)
                } else {
                    emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildRequest(url: String, token: String, eTag: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DarkModeStudio-CommandCenter")

        if (eTag != null) {
            builder.header("If-None-Match", eTag)
        }
        return builder.build()
    }
}
