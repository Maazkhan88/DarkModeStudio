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
    NOT_MODIFIED,
    AUTH_FAILURE,
    RATE_LIMITED,
    NETWORK_FAILURE,
    SERVER_FAILURE,
    NO_CREDENTIALS
}

enum class GitHubTelemetryComponent {
    REPOSITORIES,
    COMMITS,
    PULL_REQUESTS,
    WORKFLOWS
}

data class GitHubRepoTelemetryFailure(
    val repository: String,
    val component: GitHubTelemetryComponent,
    val httpCode: Int?,
    val message: String
)

data class GitHubTelemetryResult(
    val status: GitHubSyncStatus,
    val isSuccess: Boolean,
    val isNotModified: Boolean = false,
    val repos: List<GitHubRepoDto> = emptyList(),
    val commitsByRepo: Map<String, List<GitHubCommitDto>> = emptyMap(),
    val pullsByRepo: Map<String, List<GitHubPullDto>> = emptyMap(),
    val workflowsByRepo: Map<String, GitHubWorkflowRunsResponseDto> = emptyMap(),
    val failures: List<GitHubRepoTelemetryFailure> = emptyList(),
    val rateLimitRemaining: Int = 5000,
    val rateLimitResetAt: Long = 0,
    val errorMessage: String? = null
)

sealed interface GitHubContentsResult {
    data class Success(val entries: List<GitHubContentDto>) : GitHubContentsResult
    data object NoCredentials : GitHubContentsResult
    data class AuthFailure(val message: String) : GitHubContentsResult
    data class RateLimited(val resetAt: Long?) : GitHubContentsResult
    data class NetworkFailure(val message: String) : GitHubContentsResult
    data class ServerFailure(val code: Int) : GitHubContentsResult
}

class GitHubConnector(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val eTagCache = ConcurrentHashMap<String, String>()

    fun clearETagCache() {
        eTagCache.clear()
    }

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
                        return@withContext GitHubTelemetryResult(
                            status = GitHubSyncStatus.NOT_MODIFIED,
                            isSuccess = true,
                            isNotModified = true,
                            rateLimitRemaining = rateLimitRemaining,
                            rateLimitResetAt = rateLimitReset
                        )
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
            val failures = mutableListOf<GitHubRepoTelemetryFailure>()

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
                            failures.add(
                                GitHubRepoTelemetryFailure(
                                    repository = fullName,
                                    component = GitHubTelemetryComponent.COMMITS,
                                    httpCode = resp.code,
                                    message = "Failed to fetch commits (${resp.code})"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    failures.add(
                        GitHubRepoTelemetryFailure(
                            repository = fullName,
                            component = GitHubTelemetryComponent.COMMITS,
                            httpCode = null,
                            message = e.message ?: "Network error fetching commits"
                        )
                    )
                }

                // Fetch Pull Requests
                try {
                    val pullsReq = buildRequest("https://api.github.com/repos/$fullName/pulls?state=open&per_page=10", token)
                    okHttpClient.newCall(pullsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "[]"
                            pullsMap[fullName] = json.decodeFromString(body)
                        } else {
                            failures.add(
                                GitHubRepoTelemetryFailure(
                                    repository = fullName,
                                    component = GitHubTelemetryComponent.PULL_REQUESTS,
                                    httpCode = resp.code,
                                    message = "Failed to fetch pull requests (${resp.code})"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    failures.add(
                        GitHubRepoTelemetryFailure(
                            repository = fullName,
                            component = GitHubTelemetryComponent.PULL_REQUESTS,
                            httpCode = null,
                            message = e.message ?: "Network error fetching pull requests"
                        )
                    )
                }

                // Fetch Actions Workflow Runs
                try {
                    val workflowsReq = buildRequest("https://api.github.com/repos/$fullName/actions/runs?per_page=10", token)
                    okHttpClient.newCall(workflowsReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: "{}"
                            workflowsMap[fullName] = json.decodeFromString(body)
                        } else {
                            failures.add(
                                GitHubRepoTelemetryFailure(
                                    repository = fullName,
                                    component = GitHubTelemetryComponent.WORKFLOWS,
                                    httpCode = resp.code,
                                    message = "Failed to fetch workflow runs (${resp.code})"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    failures.add(
                        GitHubRepoTelemetryFailure(
                            repository = fullName,
                            component = GitHubTelemetryComponent.WORKFLOWS,
                            httpCode = null,
                            message = e.message ?: "Network error fetching workflow runs"
                        )
                    )
                }
            }

            val hasPartialFailures = failures.isNotEmpty()

            GitHubTelemetryResult(
                status = if (hasPartialFailures) GitHubSyncStatus.PARTIAL_SUCCESS else GitHubSyncStatus.SUCCESS,
                isSuccess = true,
                isNotModified = isNotModified,
                repos = repos,
                commitsByRepo = commitsMap,
                pullsByRepo = pullsMap,
                workflowsByRepo = workflowsMap,
                failures = failures,
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
        path: String = "",
        branch: String? = null
    ): GitHubContentsResult = withContext(Dispatchers.IO) {
        if (token.isNullOrBlank()) return@withContext GitHubContentsResult.NoCredentials
        val cleanPath = path.trim().trimStart('/')
        val baseUrl = if (cleanPath.isBlank()) {
            "https://api.github.com/repos/$fullName/contents"
        } else {
            "https://api.github.com/repos/$fullName/contents/$cleanPath"
        }
        val url = if (!branch.isNullOrBlank()) {
            "$baseUrl?ref=$branch"
        } else {
            baseUrl
        }

        try {
            val request = buildRequest(url, token)
            okHttpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 401 -> GitHubContentsResult.AuthFailure("Authentication failed (401)")
                    response.code == 403 -> {
                        val resetAt = response.header("x-ratelimit-reset")?.toLongOrNull()
                        GitHubContentsResult.RateLimited(resetAt)
                    }
                    response.code >= 500 -> GitHubContentsResult.ServerFailure(response.code)
                    response.isSuccessful -> {
                        val body = response.body?.string() ?: "[]"
                        try {
                            val entries: List<GitHubContentDto> = json.decodeFromString(body)
                            GitHubContentsResult.Success(entries)
                        } catch (_: Exception) {
                            // If response is a single file object instead of array
                            try {
                                val singleEntry: GitHubContentDto = json.decodeFromString(body)
                                GitHubContentsResult.Success(listOf(singleEntry))
                            } catch (parseEx: Exception) {
                                GitHubContentsResult.NetworkFailure("Failed to parse contents JSON: ${parseEx.message}")
                            }
                        }
                    }
                    else -> GitHubContentsResult.NetworkFailure("HTTP error ${response.code}: ${response.message}")
                }
            }
        } catch (e: IOException) {
            GitHubContentsResult.NetworkFailure(e.message ?: "Network error fetching contents")
        } catch (e: Exception) {
            GitHubContentsResult.NetworkFailure(e.message ?: "Error fetching contents")
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
