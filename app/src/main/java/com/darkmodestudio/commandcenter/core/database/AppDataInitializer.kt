package com.darkmodestudio.commandcenter.core.database

import androidx.room.withTransaction
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationRuleEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth

/**
 * AppDataInitializer:
 * Single, deterministic first-run initialization boundary.
 *
 * Runs inside a database transaction on application startup before any background
 * or foreground provider synchronization can occur.
 *
 * Only seeds required structural defaults (app_settings, disconnected integrations, automation rules, agent definitions).
 * NEVER silently fabricates fake user projects, tasks, commits, or quotas.
 * All default insertions are insert-only: user customizations and persisted telemetry are NEVER overwritten.
 */
class AppDataInitializer(
    private val database: DmsDatabase
) {
    suspend fun initialize() {
        database.withTransaction {
            // 1. App Settings (insert-only)
            val settings = database.settingsDao().getSettings()
            if (settings == null) {
                database.settingsDao().insertOrUpdate(AppSettingsEntity(schemaSeedVersion = 5))
            }

            // 2. Structural Default Integrations (Disconnected by default, insert-only)
            val defaultIntegrations = listOf(
                IntegrationEntity("github", "GitHub", "Code & CI/CD", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure"),
                IntegrationEntity("cloudflare", "Cloudflare", "Edge & Infrastructure", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure"),
                IntegrationEntity("firebase", "Firebase", "Mobile & Crashlytics", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure"),
                IntegrationEntity("play_console", "Google Play Console", "Distribution", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure"),
                IntegrationEntity("supabase", "Supabase", "Database & Auth", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure"),
                IntegrationEntity("vercel", "Vercel", "Frontend & Edge", isConnected = false, health = IntegrationHealth.DISCONNECTED, lastSync = "Not synced", primaryMetric = "Not connected — Tap to configure")
            )

            for (integration in defaultIntegrations) {
                if (database.integrationDao().getIntegrationById(integration.id) == null) {
                    database.integrationDao().insertIntegration(integration)
                }
            }

            // 3. Default Automation Rules (Structural rules, insert-only)
            val rules = listOf(
                AutomationRuleEntity("rule1", "GitHub Action Failure Alert", "GITHUB_WORKFLOW_FAILED", "github", null, "SEND_NOTIFICATION", true, "WHEN GitHub Action fails THEN notify me"),
                AutomationRuleEntity("rule2", "Inactivity Watchdog", "PROJECT_INACTIVE_24H", null, null, "SEND_NOTIFICATION", true, "WHEN project has no activity for 24h THEN notify me"),
                AutomationRuleEntity("rule3", "Agent Quota Warning", "AGENT_QUOTA_HIGH", null, null, "SEND_NOTIFICATION", true, "WHEN agent quota > 80% THEN notify me"),
                AutomationRuleEntity("rule4", "Auto Mark Deployed Tasks", "DEPLOYMENT_SUCCESS", "cloudflare", null, "MARK_TASK_COMPLETE", true, "WHEN deployment succeeds THEN mark deployment task complete")
            )
            for (rule in rules) {
                if (database.automationDao().getRuleById(rule.id) == null) {
                    database.automationDao().insertRule(rule)
                }
            }

            // 4. Default Agent Structural Definitions (insert-only: preserves persisted local runs/messages/tasks)
            val defaultAgents = listOf(
                AgentEntity(
                    id = "codex",
                    name = "Codex",
                    provider = AgentProvider.OPENAI,
                    mode = "Pro",
                    speed = "Fast",
                    runsUsed = 0,
                    runsTotal = 500,
                    messagesUsed = 0,
                    messagesTotal = 5000,
                    tasksUsed = 0,
                    tasksTotal = 100,
                    currentTask = "Standby",
                    statusText = "Standby • 0%",
                    usagePercentage = 0.0f
                ),
                AgentEntity(
                    id = "claude",
                    name = "Claude",
                    provider = AgentProvider.ANTHROPIC,
                    mode = "Opus",
                    speed = "Pro",
                    runsUsed = 0,
                    runsTotal = 600,
                    messagesUsed = 0,
                    messagesTotal = 10000,
                    tasksUsed = 0,
                    tasksTotal = 300,
                    currentTask = "Standby",
                    statusText = "Standby • 0%",
                    usagePercentage = 0.0f
                ),
                AgentEntity(
                    id = "antigravity",
                    name = "Antigravity",
                    provider = AgentProvider.ANTIGRAVITY,
                    mode = "Swarm",
                    speed = "Max",
                    runsUsed = 0,
                    runsTotal = 400,
                    messagesUsed = 0,
                    messagesTotal = 5000,
                    tasksUsed = 0,
                    tasksTotal = 200,
                    currentTask = "Standby",
                    statusText = "Standby • 0%",
                    usagePercentage = 0.0f
                )
            )

            for (agent in defaultAgents) {
                if (database.agentDao().getAgentById(agent.id) == null) {
                    database.agentDao().insertAgent(agent)
                }
            }
        }
    }
}
