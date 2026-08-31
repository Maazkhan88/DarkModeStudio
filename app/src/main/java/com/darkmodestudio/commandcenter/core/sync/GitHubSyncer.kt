package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.network.GitHubSyncStatus
import com.darkmodestudio.commandcenter.core.network.GitHubTelemetryComponent
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import com.darkmodestudio.commandcenter.core.util.DmsTimeFormatter
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
        val nowFormatted = DmsTimeFormatter.formatNow()

        if (storedToken.isNullOrBlank()) {
            val disconnectedIntegration = IntegrationEntity(
                id = "github",
                name = "GitHub",
                category = "Code & CI/CD",
                isConnected = false,
                health = IntegrationHealth.DISCONNECTED,
                lastSync = "Not configured",
                lastSuccessfulSync = null,
                lastError = null,
                primaryMetric = "Disconnected — Tap to configure"
            )
            database.integrationDao().insertIntegration(disconnectedIntegration)

            return@withContext ProviderSyncResult(
                provider = SecureProvider.GITHUB,
                isSuccess = false,
                message = "GitHub Personal Access Token not configured"
            )
        }

        val result = gitHubConnector.fetchAllTelemetry(storedToken)

        when (result.status) {
            GitHubSyncStatus.NOT_MODIFIED -> {
                // 304 Not Modified: Remote dataset is completely unchanged.
                // Preserve all existing projects, activities, metrics, and health state.
                val existingIntegration = database.integrationDao().getIntegrationById("github")
                val preservedHealth = existingIntegration?.health ?: IntegrationHealth.OPERATIONAL
                val preservedMetric = existingIntegration?.primaryMetric ?: "Dataset unchanged (304)"

                val updatedIntegration = IntegrationEntity(
                    id = "github",
                    name = "GitHub",
                    category = "Code & CI/CD",
                    isConnected = true,
                    health = preservedHealth,
                    lastSync = nowFormatted,
                    lastSuccessfulSync = nowFormatted,
                    lastError = null,
                    primaryMetric = preservedMetric
                )
                database.integrationDao().insertIntegration(updatedIntegration)

                return@withContext ProviderSyncResult(
                    provider = SecureProvider.GITHUB,
                    isSuccess = true,
                    message = "GitHub telemetry unchanged (304 Not Modified)"
                )
            }
            GitHubSyncStatus.AUTH_FAILURE -> {
                val failedIntegration = IntegrationEntity(
                    id = "github",
                    name = "GitHub",
                    category = "Code & CI/CD",
                    isConnected = false,
                    health = IntegrationHealth.ALERT,
                    lastSync = nowFormatted,
                    lastSuccessfulSync = null,
                    lastError = result.errorMessage,
                    primaryMetric = "Invalid Token — Reconnect"
                )
                database.integrationDao().insertIntegration(failedIntegration)

                return@withContext ProviderSyncResult(
                    provider = SecureProvider.GITHUB,
                    isSuccess = false,
                    message = result.errorMessage ?: "Authentication failure"
                )
            }
            GitHubSyncStatus.RATE_LIMITED -> {
                val degradedIntegration = IntegrationEntity(
                    id = "github",
                    name = "GitHub",
                    category = "Code & CI/CD",
                    isConnected = true,
                    health = IntegrationHealth.DEGRADED,
                    lastSync = nowFormatted,
                    lastError = "Rate limited",
                    primaryMetric = "Rate Limited — Resets in ${((result.rateLimitResetAt * 1000 - System.currentTimeMillis()) / 60000).coerceAtLeast(1)}m"
                )
                database.integrationDao().insertIntegration(degradedIntegration)

                return@withContext ProviderSyncResult(
                    provider = SecureProvider.GITHUB,
                    isSuccess = false,
                    message = "GitHub rate limit exceeded"
                )
            }
            GitHubSyncStatus.NETWORK_FAILURE, GitHubSyncStatus.SERVER_FAILURE -> {
                val failedIntegration = IntegrationEntity(
                    id = "github",
                    name = "GitHub",
                    category = "Code & CI/CD",
                    isConnected = true,
                    health = IntegrationHealth.ALERT,
                    lastSync = nowFormatted,
                    lastError = result.errorMessage,
                    primaryMetric = result.errorMessage ?: "Network failure"
                )
                database.integrationDao().insertIntegration(failedIntegration)

                return@withContext ProviderSyncResult(
                    provider = SecureProvider.GITHUB,
                    isSuccess = false,
                    message = result.errorMessage ?: "Sync failure"
                )
            }
            else -> {}
        }

        // 1. Ingest Real Repositories as Real Projects in Room SQLite (NO synthetic project management fields)
        if (result.repos.isNotEmpty()) {
            for (repo in result.repos) {
                val id = repo.name.lowercase().replace(" ", "-")
                val iconTag = repo.name.take(2).uppercase()
                val existingProject = database.projectDao().getProjectById(id)
                val exactLastUpdate = DmsTimeFormatter.parseIsoToLocal(repo.pushedAt ?: repo.updatedAt) ?: "Unknown"
                val createdDate = DmsTimeFormatter.parseIsoToLocalDateOnly(repo.createdAt) ?: DmsTimeFormatter.parseIsoToLocalDateOnly(repo.pushedAt) ?: "Unknown"

                if (existingProject == null) {
                    val newProject = ProjectEntity(
                        id = id,
                        name = repo.name,
                        description = repo.description ?: "Repository ${repo.fullName}",
                        iconTag = iconTag,
                        status = ProjectStatus.IMPORTED,
                        isMvp = false,
                        owner = repo.fullName.substringBefore("/"),
                        createdAt = createdDate,
                        dueDate = "",
                        nextMilestone = "",
                        manualProgressOverride = null,
                        planningWeight = 0f,
                        developmentWeight = 0f,
                        testingWeight = 0f,
                        deploymentWeight = 0f,
                        lastUpdate = exactLastUpdate,
                        repositoryFullName = repo.fullName,
                        repositoryDefaultBranch = repo.defaultBranch
                    )
                    database.projectDao().insertProject(newProject)
                } else {
                    val updatedProject = existingProject.copy(
                        name = repo.name,
                        description = repo.description ?: existingProject.description,
                        owner = repo.fullName.substringBefore("/"),
                        createdAt = if (existingProject.createdAt.isNotBlank() && existingProject.createdAt != "Unknown") existingProject.createdAt else createdDate,
                        lastUpdate = exactLastUpdate,
                        repositoryFullName = repo.fullName,
                        repositoryDefaultBranch = repo.defaultBranch
                    )
                    database.projectDao().updateProject(updatedProject)
                }
            }
        }

        // 2. Ingest Real Commits into Project Activities with Exact UTC-to-Local Date & Time
        val newActivities = mutableListOf<ProjectActivityEntity>()
        result.commitsByRepo.forEach { (repoKey, commits) ->
            val repoName = repoKey.substringAfter("/").lowercase()
            commits.take(6).forEach { commitDto ->
                val commitDate = commitDto.commit.author?.date
                val exactTimestamp = DmsTimeFormatter.parseIsoToLocal(commitDate) ?: "Unknown"

                newActivities.add(
                    ProjectActivityEntity(
                        id = "gh_" + commitDto.sha.take(8),
                        projectId = repoName,
                        title = commitDto.commit.message.lines().firstOrNull()?.take(60) ?: "Commit",
                        author = commitDto.commit.author?.name ?: "Contributor",
                        hash = commitDto.sha.take(7),
                        timestamp = exactTimestamp
                    )
                )
            }
        }
        if (newActivities.isNotEmpty()) {
            database.projectDao().insertActivities(newActivities)
        }

        // 3. Compute Real Workflows & Determine Health
        var totalFailing = 0
        var totalPassing = 0
        result.workflowsByRepo.forEach { (_, workflows) ->
            workflows.workflowRuns.forEach { run ->
                if (run.conclusion == "failure") totalFailing++
                else if (run.conclusion == "success") totalPassing++
            }
        }

        val isPartial = result.status == GitHubSyncStatus.PARTIAL_SUCCESS || result.failures.isNotEmpty()
        val health = when {
            totalFailing > 0 -> IntegrationHealth.DEGRADED
            isPartial -> IntegrationHealth.DEGRADED
            else -> IntegrationHealth.OPERATIONAL
        }

        val primaryMetric = when {
            isPartial -> "Partial Sync: ${result.failures.size} subrequests degraded (${result.repos.size} Repos)"
            totalFailing > 0 -> "$totalFailing CI Actions Failing"
            else -> "All CI Actions Passing (${result.repos.size} Repos)"
        }
        val primaryRepoName = result.repos.firstOrNull()?.fullName ?: "Configured GitHub Account"

        // 4. Read existing metrics before touching IntegrationEntity (avoids CASCADE delete loss)
        val existingMetrics = database.integrationDao().getMetricsByIntegration("github").associateBy { it.label }
        val prFailed = result.failures.any { it.component == GitHubTelemetryComponent.PULL_REQUESTS }
        val workflowsFailed = result.failures.any { it.component == GitHubTelemetryComponent.WORKFLOWS }

        val openPRMetricValue = if (prFailed && existingMetrics.containsKey("Open PRs")) {
            existingMetrics["Open PRs"]!!.value
        } else if (prFailed) {
            "Unavailable (500)"
        } else {
            val openPRCount = result.pullsByRepo.values.flatten().size
            "$openPRCount open PRs"
        }

        val workflowsMetricValue = if (workflowsFailed && existingMetrics.containsKey("Workflows")) {
            existingMetrics["Workflows"]!!.value
        } else if (workflowsFailed) {
            "Unavailable (500)"
        } else {
            "$totalPassing passing / $totalFailing failing"
        }

        val latestPushIso = result.repos.mapNotNull { it.pushedAt }.maxOrNull()
        val lastPushFormatted = latestPushIso?.let { DmsTimeFormatter.parseIsoToLocal(it) } ?: "No push history"

        // 5. Update Integration Entity
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

        // 6. Update Metrics
        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "github", label = "Primary Repo", value = primaryRepoName),
            IntegrationMetricEntity(integrationId = "github", label = "Last Push", value = lastPushFormatted),
            IntegrationMetricEntity(integrationId = "github", label = "Open PRs", value = openPRMetricValue),
            IntegrationMetricEntity(integrationId = "github", label = "Workflows", value = workflowsMetricValue)
        )
        database.integrationDao().insertMetrics(metrics)

        // 7. Ingest Incident if failing CI
        if (totalFailing > 0) {
            database.integrationDao().insertIncidents(
                listOf(
                    IntegrationIncidentEntity(
                        id = "gh_inc_" + System.currentTimeMillis(),
                        integrationId = "github",
                        title = "GitHub Actions workflow run failed on main",
                        description = "CI build pipeline failure detected across synced repos",
                        timestamp = nowFormatted,
                        isResolved = false
                    )
                )
            )
        }

        ProviderSyncResult(
            provider = SecureProvider.GITHUB,
            isSuccess = true,
            message = if (isPartial) "GitHub telemetry synced with partial failures (${result.failures.size} failed subrequests)" else "Live GitHub telemetry synchronized (${result.repos.size} repos, ${newActivities.size} commits)"
        )
    }
}
