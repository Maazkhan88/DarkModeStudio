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
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import com.darkmodestudio.commandcenter.core.security.SecureProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitHubSyncer(
    private val database: DmsDatabase,
    private val keystoreCredentialManager: KeystoreCredentialManager,
    private val gitHubConnector: GitHubConnector = GitHubConnector()
) : ProviderSyncer {

    override val provider: SecureProvider = SecureProvider.GITHUB

    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    private fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return displayDateFormat.format(Date())
        return try {
            val parsed = isoDateFormat.parse(isoString)
            if (parsed != null) displayDateFormat.format(parsed) else displayDateFormat.format(Date())
        } catch (_: Exception) {
            displayDateFormat.format(Date())
        }
    }

    override suspend fun sync(mode: SyncMode): ProviderSyncResult = withContext(Dispatchers.IO) {
        val storedToken = keystoreCredentialManager.getSecret("token_github") ?: ""
        val result = gitHubConnector.fetchAllTelemetry(storedToken)

        if (!result.isSuccess && result.errorMessage?.contains("401") == true) {
            return@withContext ProviderSyncResult(
                provider = SecureProvider.GITHUB,
                isSuccess = false,
                message = "Invalid GitHub token"
            )
        }

        val nowFormatted = displayDateFormat.format(Date())

        // 1. Ingest Real Repositories as Real Projects in Room SQLite
        if (result.repos.isNotEmpty()) {
            val liveProjects = result.repos.map { repo ->
                val id = repo.name.lowercase().replace(" ", "-")
                val iconTag = repo.name.take(2).uppercase()
                val latestCommit = result.commitsByRepo[repo.fullName]?.firstOrNull()
                val latestMessage = latestCommit?.commit?.message?.lines()?.firstOrNull()?.take(40) ?: "Active Development"

                ProjectEntity(
                    id = id,
                    name = repo.name,
                    description = repo.description ?: "Repository ${repo.fullName}",
                    iconTag = iconTag,
                    status = ProjectStatus.ON_TRACK,
                    isMvp = repo.name.contains("SecondMe", ignoreCase = true) || repo.name.contains("DarkModeStudio", ignoreCase = true),
                    owner = "Maazkhan88",
                    createdAt = repo.pushedAt?.take(10) ?: repo.updatedAt?.take(10) ?: "Aug 2026",
                    dueDate = "Q4 2026",
                    nextMilestone = latestMessage,
                    manualProgressOverride = 0.65f,
                    lastUpdate = formatTimestamp(repo.pushedAt ?: repo.updatedAt)
                )
            }
            database.projectDao().insertProjects(liveProjects)
        }

        // 2. Ingest Real Commits into Project Activities with Exact Date & Time
        val newActivities = mutableListOf<ProjectActivityEntity>()
        result.commitsByRepo.forEach { (repoKey, commits) ->
            val repoName = repoKey.substringAfter("/").lowercase()
            commits.take(6).forEach { commitDto ->
                newActivities.add(
                    ProjectActivityEntity(
                        id = "gh_" + commitDto.sha.take(8),
                        projectId = repoName,
                        title = commitDto.commit.message.lines().firstOrNull()?.take(60) ?: "Commit",
                        author = commitDto.commit.author?.name ?: "Maazkhan88",
                        hash = commitDto.sha.take(7),
                        timestamp = formatTimestamp(commitDto.commit.author?.date)
                    )
                )
            }
        }
        if (newActivities.isNotEmpty()) {
            database.projectDao().insertActivities(newActivities)
        }

        // 3. Compute Real Workflows & Health
        var totalFailing = 0
        var totalPassing = 0
        result.workflowsByRepo.forEach { (_, workflows) ->
            workflows.workflowRuns.forEach { run ->
                if (run.conclusion == "failure") totalFailing++
                else if (run.conclusion == "success") totalPassing++
            }
        }

        val health = if (totalFailing > 0) IntegrationHealth.DEGRADED else IntegrationHealth.OPERATIONAL
        val primaryMetric = if (totalFailing > 0) "$totalFailing CI Actions Failing" else "All CI Actions Passing (${result.repos.size} Repos)"
        val primaryRepoName = result.repos.firstOrNull()?.fullName ?: "Maazkhan88/DarkModeStudio"

        // 4. Update Integration Entity
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

        // 5. Update Metrics
        val metrics = listOf(
            IntegrationMetricEntity(integrationId = "github", label = "Primary Repo", value = primaryRepoName),
            IntegrationMetricEntity(integrationId = "github", label = "Last Push", value = nowFormatted),
            IntegrationMetricEntity(integrationId = "github", label = "Open PRs", value = "${result.pullsByRepo.values.flatten().size} open PRs"),
            IntegrationMetricEntity(integrationId = "github", label = "Workflows", value = "$totalPassing passing / $totalFailing failing")
        )
        database.integrationDao().insertMetrics(metrics)

        // 6. Ingest Incident if failing CI
        if (totalFailing > 0) {
            database.integrationDao().insertIncidents(
                listOf(
                    IntegrationIncidentEntity(
                        id = "gh_inc_" + System.currentTimeMillis(),
                        integrationId = "github",
                        title = "GitHub Actions workflow run failed on main",
                        description = "CI build pipeline failure detected",
                        timestamp = nowFormatted,
                        isResolved = false
                    )
                )
            )
        }

        ProviderSyncResult(
            provider = SecureProvider.GITHUB,
            isSuccess = true,
            message = "Live GitHub telemetry synchronized (${result.repos.size} real repos, ${newActivities.size} real commits)"
        )
    }
}
