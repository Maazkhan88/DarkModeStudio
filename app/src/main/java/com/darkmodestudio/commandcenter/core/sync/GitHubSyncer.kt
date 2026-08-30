package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubSyncer(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val gitHubConnector: GitHubConnector = GitHubConnector()
) : ProviderSyncer {

    override val provider: SecureProvider = SecureProvider.GITHUB

    override suspend fun sync(mode: SyncMode): ProviderSyncResult = withContext(Dispatchers.IO) {
        val storedToken = keystoreCredentialManager.getSecret("token_github")
        val isTokenPresent = !storedToken.isNullOrBlank()

        if (!isTokenPresent) {
            val isConnected = keystoreCredentialManager.hasSecret("token_github")
            return@withContext ProviderSyncResult(
                provider = SecureProvider.GITHUB,
                isSuccess = true,
                message = "GitHub waiting for PAT credential (offline mode active)"
            )
        }

        // Fetch User Repositories with live token
        val liveResult = gitHubConnector.fetchAllTelemetry(storedToken!!)

        if (!liveResult.isSuccess && liveResult.errorMessage?.contains("401") == true) {
            val integration = IntegrationEntity(
                id = "github",
                name = "GitHub",
                category = "Code & CI/CD",
                isConnected = false,
                health = IntegrationHealth.DEGRADED,
                lastSync = "Auth Error",
                lastSuccessfulSync = "Pending Auth",
                primaryMetric = "Invalid Token (401)"
            )
            database.integrationDao().insertIntegration(integration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.GITHUB,
                isSuccess = false,
                message = "Invalid or expired GitHub Personal Access Token"
            )
        }

        val nowFormatted = "Just now"

        // 1. Ingest Commits into Project Activities
        val newActivities = mutableListOf<ProjectActivityEntity>()
        liveResult.commitsByRepo.forEach { (repoKey, commits) ->
            val projectId = if (repoKey.contains("ghostcart", ignoreCase = true)) "ghostcart" else "secondme"
            commits.take(5).forEach { commitDto ->
                newActivities.add(
                    ProjectActivityEntity(
                        id = "gh_" + commitDto.sha.take(8),
                        projectId = projectId,
                        title = commitDto.commit.message.lines().firstOrNull()?.take(50) ?: "Commit",
                        author = commitDto.commit.author?.name ?: "GitHub",
                        hash = commitDto.sha.take(7),
                        timestamp = "Live"
                    )
                )
            }
        }
        if (newActivities.isNotEmpty()) {
            database.projectDao().insertActivities(newActivities)
        }

        // 2. Compute Workflows & Health
        var totalFailing = 0
        var totalPassing = 0
        liveResult.workflowsByRepo.forEach { (_, workflows) ->
            workflows.workflowRuns.forEach { run ->
                if (run.conclusion == "failure") totalFailing++
                else if (run.conclusion == "success") totalPassing++
            }
        }

        val health = if (totalFailing > 0) IntegrationHealth.DEGRADED else IntegrationHealth.OPERATIONAL
        val primaryMetric = if (totalFailing > 0) "$totalFailing CI Actions Failing" else "All CI Actions Passing"

        val primaryRepoName = liveResult.repos.firstOrNull()?.fullName ?: "darkmodestudio/core"

        // 3. Update Integration Entity
        val updatedIntegration = IntegrationEntity(
            id = "github",
            name = "GitHub",
            category = "Code & CI/CD",
            isConnected = true,
            health = health,
            lastSync = nowFormatted,
            lastSuccessfulSync = nowFormatted,
            primaryMetric = primaryMetric
        )
        database.integrationDao().insertIntegration(updatedIntegration)

        // 4. Update Metrics
        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "github", label = "Primary Repo", value = primaryRepoName),
            IntegrationMetricEntity(integrationId = "github", label = "Last Push", value = "Live (main)"),
            IntegrationMetricEntity(integrationId = "github", label = "Open PRs", value = "${liveResult.pullsByRepo.values.flatten().size} open PRs"),
            IntegrationMetricEntity(integrationId = "github", label = "Workflows", value = "$totalPassing passing / $totalFailing failing")
        )
        database.integrationDao().insertMetrics(metrics)

        // 5. Ingest Incident if failing CI
        if (totalFailing > 0) {
            database.integrationDao().insertIncidents(
                listOf(
                    IntegrationIncidentEntity(
                        id = "gh_inc_" + System.currentTimeMillis(),
                        integrationId = "github",
                        title = "GitHub Actions workflow run failed on main",
                        description = "CI build pipeline failure detected",
                        timestamp = "Just now",
                        isResolved = false
                    )
                )
            )
        }

        ProviderSyncResult(
            provider = SecureProvider.GITHUB,
            isSuccess = true,
            message = "Live GitHub telemetry synchronized (${liveResult.repos.size} repos, ${liveResult.commitsByRepo.values.flatten().size} commits)"
        )
    }
}
