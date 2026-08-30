package com.darkmodestudio.commandcenter.core.database

import androidx.room.withTransaction
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationRuleEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth

/**
 * AppDataInitializer:
 * Single, deterministic first-run initialization boundary.
 *
 * Runs inside a database transaction on application startup before any background
 * or foreground provider synchronization can occur.
 *
 * Only seeds required structural defaults (app_settings, disconnected integrations, automation rules).
 * NEVER silently fabricates fake user projects, tasks, commits, or quotas.
 */
class AppDataInitializer(
    private val database: DmsDatabase
) {
    suspend fun initialize() {
        database.withTransaction {
            // 1. App Settings
            val settings = database.settingsDao().getSettings()
            if (settings == null) {
                database.settingsDao().insertOrUpdate(AppSettingsEntity(schemaSeedVersion = 4))
            }

            // 2. Structural Default Integrations (Disconnected by default)
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

            // 3. Default Automation Rules (Structural rules)
            val rules = listOf(
                AutomationRuleEntity("rule1", "GitHub Action Failure Alert", "GITHUB_WORKFLOW_FAILED", "github", null, "SEND_NOTIFICATION", true, "WHEN GitHub Action fails THEN notify me"),
                AutomationRuleEntity("rule2", "Inactivity Watchdog", "PROJECT_INACTIVE_24H", null, null, "SEND_NOTIFICATION", true, "WHEN project has no activity for 24h THEN notify me"),
                AutomationRuleEntity("rule3", "Agent Quota Warning", "AGENT_QUOTA_HIGH", null, null, "SEND_NOTIFICATION", true, "WHEN agent quota > 80% THEN notify me"),
                AutomationRuleEntity("rule4", "Auto Mark Deployed Tasks", "DEPLOYMENT_SUCCESS", "cloudflare", null, "MARK_TASK_COMPLETE", true, "WHEN deployment succeeds THEN mark deployment task complete")
            )
            database.automationDao().insertRules(rules)
        }
    }
}
