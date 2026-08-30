package com.darkmodestudio.commandcenter.core.network

import com.darkmodestudio.commandcenter.core.network.model.GitHubCommitDetailDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubCommitDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubPullDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubRepoDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubWorkflowRunDto
import com.darkmodestudio.commandcenter.core.network.model.GitHubWorkflowRunsResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubConnectorTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun testGitHubRepoDtoParsing() {
        val repoJson = """
            {
                "id": 123456,
                "name": "darkmodestudio",
                "full_name": "darkmodestudio/core",
                "private": true,
                "default_branch": "main",
                "open_issues_count": 5,
                "pushed_at": "2026-08-30T10:00:00Z"
            }
        """.trimIndent()

        val repo: GitHubRepoDto = json.decodeFromString(repoJson)
        assertEquals(123456L, repo.id)
        assertEquals("darkmodestudio", repo.name)
        assertEquals("darkmodestudio/core", repo.fullName)
        assertTrue(repo.private)
        assertEquals("main", repo.defaultBranch)
        assertEquals(5, repo.openIssuesCount)
    }

    @Test
    fun testGitHubCommitDtoParsing() {
        val commitJson = """
            {
                "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
                "commit": {
                    "author": {
                        "name": "Moneeb",
                        "email": "moneeb@example.com",
                        "date": "2026-08-30T11:00:00Z"
                    },
                    "message": "feat: connect live GitHub Actions CI telemetry"
                }
            }
        """.trimIndent()

        val commit: GitHubCommitDto = json.decodeFromString(commitJson)
        assertEquals("6dcb09b5b57875f334f61aebed695e2e4193db5e", commit.sha)
        assertEquals("Moneeb", commit.commit.author?.name)
        assertEquals("feat: connect live GitHub Actions CI telemetry", commit.commit.message)
    }

    @Test
    fun testGitHubWorkflowRunsParsing() {
        val workflowJson = """
            {
                "total_count": 2,
                "workflow_runs": [
                    {
                        "id": 98765,
                        "name": "Build & Verify",
                        "status": "completed",
                        "conclusion": "success",
                        "created_at": "2026-08-30T09:00:00Z",
                        "updated_at": "2026-08-30T09:05:00Z"
                    },
                    {
                        "id": 98766,
                        "name": "Release Binary",
                        "status": "completed",
                        "conclusion": "success",
                        "created_at": "2026-08-30T09:10:00Z",
                        "updated_at": "2026-08-30T09:15:00Z"
                    }
                ]
            }
        """.trimIndent()

        val workflows: GitHubWorkflowRunsResponseDto = json.decodeFromString(workflowJson)
        assertEquals(2, workflows.totalCount)
        assertEquals(2, workflows.workflowRuns.size)
        assertEquals("success", workflows.workflowRuns.first().conclusion)
    }

    @Test
    fun testGitHubPullDtoParsing() {
        val pullJson = """
            {
                "id": 554433,
                "number": 42,
                "title": "Add biometric fallback to auth layer",
                "state": "open",
                "created_at": "2026-08-29T18:00:00Z",
                "updated_at": "2026-08-30T08:00:00Z"
            }
        """.trimIndent()

        val pull: GitHubPullDto = json.decodeFromString(pullJson)
        assertEquals(42, pull.number)
        assertEquals("Add biometric fallback to auth layer", pull.title)
        assertEquals("open", pull.state)
    }
}
