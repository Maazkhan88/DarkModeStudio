package com.darkmodestudio.commandcenter.core.sync

import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.AutomationExecutionEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationState
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AutomationEvaluator(private val database: DmsDatabase) {

    suspend fun evaluateAllRules(): Int = withContext(Dispatchers.IO) {
        val rules = database.automationDao().getAutomationRulesFlow().first()
        var triggeredCount = 0

        for (rule in rules.filter { it.isEnabled }) {
            when (rule.triggerType) {
                "GITHUB_WORKFLOW_FAILED" -> {
                    val github = database.integrationDao().getIntegrationsWithDetailsFlow().first()
                        .find { it.integration.id == "github" }

                    val hasFailedWorkflow = github?.metrics?.any { it.label == "Workflows" && it.value.contains("failing") && !it.value.startsWith("0") } ?: false

                    if (hasFailedWorkflow) {
                        val eventId = "event_ci_fail_" + System.currentTimeMillis() / (1000 * 60 * 60) // Dedup per hour
                        val execution = AutomationExecutionEntity(
                            id = "exec_" + System.currentTimeMillis(),
                            ruleId = rule.id,
                            result = "SUCCESS",
                            message = "Generated build alert notification for failed CI",
                            sourceEventId = eventId
                        )
                        database.automationDao().insertExecution(execution)

                        val notif = NotificationEntity(
                            id = "notif_ci_" + System.currentTimeMillis(),
                            title = "GitHub CI workflow alert",
                            description = "Automated watchdog detected CI failure on main branch",
                            timeAgo = "Just now",
                            type = NotificationType.BUILD_ALERT,
                            state = NotificationState.UNREAD
                        )
                        database.notificationDao().insertNotification(notif)
                        triggeredCount++
                    }
                }

                "AGENT_QUOTA_HIGH" -> {
                    val agents = database.agentDao().getAgentsFlow().first()
                    val highUsageAgent = agents.find { it.usagePercentage >= 0.80f }

                    if (highUsageAgent != null) {
                        val eventId = "event_quota_${highUsageAgent.id}_" + System.currentTimeMillis() / (1000 * 60 * 60 * 24) // Dedup per day
                        val execution = AutomationExecutionEntity(
                            id = "exec_" + System.currentTimeMillis(),
                            ruleId = rule.id,
                            result = "SUCCESS",
                            message = "Agent quota warning generated for ${highUsageAgent.name}",
                            sourceEventId = eventId
                        )
                        database.automationDao().insertExecution(execution)

                        val notif = NotificationEntity(
                            id = "notif_quota_" + System.currentTimeMillis(),
                            title = "${highUsageAgent.name} usage limit at ${(highUsageAgent.usagePercentage * 100).toInt()}%",
                            description = "Automated watchdog alert: approaching monthly allocation limit",
                            timeAgo = "Just now",
                            type = NotificationType.AGENT_LIMIT,
                            state = NotificationState.UNREAD
                        )
                        database.notificationDao().insertNotification(notif)
                        triggeredCount++
                    }
                }

                "DEPLOYMENT_SUCCESS" -> {
                    val tasks = database.taskDao().getTasksFlow().first()
                    val deploymentTask = tasks.find { it.title.contains("deployment", ignoreCase = true) && it.status != TaskStatus.DONE }

                    if (deploymentTask != null) {
                        database.taskDao().updateTaskStatus(deploymentTask.id, TaskStatus.DONE, "Just now")
                        val execution = AutomationExecutionEntity(
                            id = "exec_" + System.currentTimeMillis(),
                            ruleId = rule.id,
                            result = "SUCCESS",
                            message = "Auto-marked deployment task ${deploymentTask.id} as complete",
                            sourceEventId = "deploy_success_" + System.currentTimeMillis()
                        )
                        database.automationDao().insertExecution(execution)
                        triggeredCount++
                    }
                }
            }
        }

        triggeredCount
    }
}
