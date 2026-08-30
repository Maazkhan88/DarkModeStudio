package com.darkmodestudio.commandcenter.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUserDto(
    val login: String,
    val id: Long,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("total_private_repos") val totalPrivateRepos: Int = 0
)

@Serializable
data class GitHubRepoDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val private: Boolean = false,
    val description: String? = null,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
    @SerialName("pushed_at") val pushedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class GitHubCommitAuthorDto(
    val name: String? = null,
    val email: String? = null,
    val date: String? = null
)

@Serializable
data class GitHubCommitDetailDto(
    val author: GitHubCommitAuthorDto? = null,
    val message: String
)

@Serializable
data class GitHubCommitDto(
    val sha: String,
    val commit: GitHubCommitDetailDto
)

@Serializable
data class GitHubPullDto(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GitHubWorkflowRunDto(
    val id: Long,
    val name: String? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
    val status: String,
    val conclusion: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GitHubWorkflowRunsResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList()
)

@Serializable
data class GitHubRateLimitCoreDto(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val used: Int
)

@Serializable
data class GitHubRateLimitResourcesDto(
    val core: GitHubRateLimitCoreDto
)

@Serializable
data class GitHubRateLimitResponseDto(
    val resources: GitHubRateLimitResourcesDto
)
