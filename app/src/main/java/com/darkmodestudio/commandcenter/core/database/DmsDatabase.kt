package com.darkmodestudio.commandcenter.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.darkmodestudio.commandcenter.core.database.dao.AgentDao
import com.darkmodestudio.commandcenter.core.database.dao.AutomationDao
import com.darkmodestudio.commandcenter.core.database.dao.IntegrationDao
import com.darkmodestudio.commandcenter.core.database.dao.NotificationDao
import com.darkmodestudio.commandcenter.core.database.dao.ProjectDao
import com.darkmodestudio.commandcenter.core.database.dao.ReminderDao
import com.darkmodestudio.commandcenter.core.database.dao.SettingsDao
import com.darkmodestudio.commandcenter.core.database.dao.TaskDao
import com.darkmodestudio.commandcenter.core.database.entity.AgentActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationExecutionEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationRuleEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationState
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectBlockerEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectMilestoneEntity
import com.darkmodestudio.commandcenter.core.database.entity.ReminderEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProjectEntity::class,
        ProjectMilestoneEntity::class,
        ProjectBlockerEntity::class,
        ProjectActivityEntity::class,
        TaskEntity::class,
        AgentEntity::class,
        AgentUsageSnapshotEntity::class,
        AgentActivityEntity::class,
        IntegrationEntity::class,
        IntegrationMetricEntity::class,
        IntegrationIncidentEntity::class,
        NotificationEntity::class,
        ReminderEntity::class,
        AutomationRuleEntity::class,
        AutomationExecutionEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DmsDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun agentDao(): AgentDao
    abstract fun integrationDao(): IntegrationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun automationDao(): AutomationDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DmsDatabase? = null

        fun getInstance(context: Context): DmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DmsDatabase::class.java,
                    "darkmodestudio_command_center.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Trigger Initial Seed in Coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialData(instance)
                }

                instance
            }
        }

        suspend fun seedInitialData(db: DmsDatabase) {
            // Seed Real User Projects
            val projects = listOf(
                ProjectEntity(
                    id = "darkmodestudio",
                    name = "DarkModeStudio",
                    description = "Personal command center Android application",
                    iconTag = "DM",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = true,
                    owner = "Maazkhan88",
                    createdAt = "Aug 30, 2026",
                    dueDate = "Q4 2026",
                    nextMilestone = "v1.5.0 Production Live Sync",
                    manualProgressOverride = 0.94f,
                    lastUpdate = "Aug 30, 2026 • 05:00 PM"
                ),
                ProjectEntity(
                    id = "secondme",
                    name = "SecondMe",
                    description = "Second You digital twin & memory clone",
                    iconTag = "SM",
                    status = ProjectStatus.IN_PROGRESS,
                    isMvp = true,
                    owner = "Maazkhan88",
                    createdAt = "Aug 28, 2026",
                    dueDate = "Oct 2026",
                    nextMilestone = "Google OAuth & Biometric Passkey",
                    manualProgressOverride = 0.65f,
                    lastUpdate = "Aug 30, 2026 • 04:30 PM"
                ),
                ProjectEntity(
                    id = "agstudio",
                    name = "AGStudio",
                    description = "Agentic developer IDE & connection runtime",
                    iconTag = "AG",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = false,
                    owner = "Maazkhan88",
                    createdAt = "Aug 26, 2026",
                    dueDate = "Nov 2026",
                    nextMilestone = "Agent Runtime & IDE Bridge",
                    manualProgressOverride = 0.74f,
                    lastUpdate = "Aug 30, 2026 • 02:00 PM"
                ),
                ProjectEntity(
                    id = "ghostcart",
                    name = "Ghostcart",
                    description = "Ghost your cravings — stealth checkout engine",
                    iconTag = "GC",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = false,
                    owner = "Maazkhan88",
                    createdAt = "Jul 15, 2026",
                    dueDate = "Sep 2026",
                    nextMilestone = "Headless Checkout Pipeline",
                    manualProgressOverride = 0.81f,
                    lastUpdate = "Aug 30, 2026 • 03:15 PM"
                ),
                ProjectEntity(
                    id = "pioneer-auctions",
                    name = "Pioneer-Auctions",
                    description = "Real-time auction and bidding platform",
                    iconTag = "PA",
                    status = ProjectStatus.IN_PROGRESS,
                    isMvp = false,
                    owner = "Maazkhan88",
                    createdAt = "Jul 20, 2026",
                    dueDate = "Dec 2026",
                    nextMilestone = "WebSocket Bid Stream",
                    manualProgressOverride = 0.58f,
                    lastUpdate = "Aug 29, 2026 • 11:00 AM"
                ),
                ProjectEntity(
                    id = "avero",
                    name = "Avero",
                    description = "Financial Decision Intelligence",
                    iconTag = "AV",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = false,
                    owner = "Maazkhan88",
                    createdAt = "Aug 24, 2026",
                    dueDate = "Q1 2027",
                    nextMilestone = "Decision Engine v1",
                    manualProgressOverride = 0.45f,
                    lastUpdate = "Aug 27, 2026 • 09:00 AM"
                )
            )
            db.projectDao().insertProjects(projects)

            // Seed Milestones
            val smMilestones = listOf(
                ProjectMilestoneEntity("sm_m1", "secondme", "Architecture", isCompleted = true, isActive = false, date = "Aug 10, 2026", sortOrder = 0),
                ProjectMilestoneEntity("sm_m2", "secondme", "Memory Engine", isCompleted = true, isActive = false, date = "Aug 22, 2026", sortOrder = 1),
                ProjectMilestoneEntity("sm_m3", "secondme", "Google OAuth & Biometrics", isCompleted = false, isActive = true, date = "Sep 05, 2026", sortOrder = 2),
                ProjectMilestoneEntity("sm_m4", "secondme", "Staging v1.0", isCompleted = false, isActive = false, date = "Sep 20, 2026", sortOrder = 3)
            )
            db.projectDao().insertMilestones(smMilestones)

            // Seed Blockers
            val blockers = listOf(
                ProjectBlockerEntity("b1", "secondme", "Waiting on Cloudflare Workers AI rate-limit tier increase", "High", "Active since Aug 29, 2026 • 02:00 PM"),
                ProjectBlockerEntity("b2", "pioneer-auctions", "WebSocket reconnection retry jitter under load", "High", "Active since Aug 28, 2026 • 09:30 AM")
            )
            db.projectDao().insertBlockers(blockers)

            // Seed Tasks with Exact Date & Time
            val tasks = listOf(
                TaskEntity("t1", "darkmodestudio", "DarkModeStudio", "Deploy v1.5.0 with live GitHub API", "Verify commit streaming and OLED monochrome theme", TaskStatus.PENDING, TaskPriority.HIGH, "Antigravity", "Aug 30, 2026 • 06:00 PM"),
                TaskEntity("t2", "secondme", "SecondMe", "Verify Google OAuth Web Client ID", "Test Android Credential Manager sign-in lifecycle", TaskStatus.PENDING, TaskPriority.HIGH, "Codex", "Aug 30, 2026 • 07:30 PM"),
                TaskEntity("t3", "agstudio", "AGStudio", "Connect Agent Runtime to Local Bridge", "Stream CLI token meters and active task events", TaskStatus.PENDING, TaskPriority.MEDIUM, "Antigravity", "Aug 31, 2026 • 10:00 AM"),
                TaskEntity("t4", "ghostcart", "Ghostcart", "Verify headless checkout edge response", "Ensure sub-50ms latency across Cloudflare workers", TaskStatus.DONE, TaskPriority.HIGH, "Claude", "Aug 30, 2026 • 03:15 PM", completedAt = "Aug 30, 2026 • 03:15 PM"),
                TaskEntity("t5", "avero", "Avero", "Decision intelligence graph benchmark", "Benchmark latency across 10,000 portfolio nodes", TaskStatus.PENDING, TaskPriority.HIGH, "Claude", "Sep 01, 2026 • 02:00 PM"),
                TaskEntity("t6", "pioneer-auctions", "Pioneer-Auctions", "Real-time WebSocket auction bid engine", "Resolve reconnection retry jitter under high concurrency", TaskStatus.BLOCKED, TaskPriority.HIGH, "Codex", "Aug 29, 2026 • 11:00 AM")
            )
            db.taskDao().insertTasks(tasks)

            // Seed Agents
            val agents = listOf(
                AgentEntity("codex", "Codex (CLI)", AgentProvider.OPENAI, "Pro", "Fast", 225, 500, 2150, 5000, 48, 100, "Refactor auth module & Credential Manager", "In Progress • 68%", 0.68f),
                AgentEntity("claude", "Claude 3.5 Sonnet", AgentProvider.ANTHROPIC, "Opus", "Pro", 160, 600, 4320, 10000, 112, 300, "Synthesize SecondMe API schema and edge endpoints", "Active • 82%", 0.82f),
                AgentEntity("antigravity", "Antigravity", AgentProvider.ANTIGRAVITY, "Swarm", "Max", 94, 400, 2150, 5000, 53, 200, "Orchestrate multi-agent build pipeline & QA review", "Ready • 41%", 0.41f),
                AgentEntity("gemini", "Gemini 1.5 Pro", AgentProvider.CUSTOM, "Flash", "Fast", 55, 300, 1000, 2000, 18, 100, "Analyze CI workflow logs and failure traces", "Idle • 18%", 0.18f)
            )
            db.agentDao().insertAgents(agents)

            // Seed Integrations
            val integrations = listOf(
                IntegrationEntity("github", "GitHub", "Code & CI/CD", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "All CI Actions Passing"),
                IntegrationEntity("cloudflare", "Cloudflare", "Edge & Infrastructure", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "1.8M req • 0.01% err"),
                IntegrationEntity("firebase", "Firebase", "Mobile & Crashlytics", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "99.98% Crash-Free"),
                IntegrationEntity("play_console", "Google Play Console", "Distribution", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "Internal Track Active"),
                IntegrationEntity("supabase", "Supabase", "Database & Auth", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "28ms Latency • Pool 42%"),
                IntegrationEntity("vercel", "Vercel", "Frontend & Edge", isConnected = true, health = IntegrationHealth.OPERATIONAL, lastSync = "Aug 30, 2026 • 05:00 PM", primaryMetric = "secondme.ai • Ready")
            )
            db.integrationDao().insertIntegrations(integrations)

            val metrics = listOf(
                IntegrationMetricEntity(integrationId = "github", label = "Primary Repo", value = "Maazkhan88/DarkModeStudio"),
                IntegrationMetricEntity(integrationId = "github", label = "Last Push", value = "Aug 30, 2026 • 04:58 PM"),
                IntegrationMetricEntity(integrationId = "github", label = "Open PRs", value = "2 open PRs"),
                IntegrationMetricEntity(integrationId = "github", label = "Workflows", value = "All passing"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Daily Requests", value = "1,842,910"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Error Rate", value = "0.01% (nominal)"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Cache Hit Ratio", value = "96.4%"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Workers Status", value = "14 workers active"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Database Latency", value = "28ms"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Connection Pool", value = "42% active"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Storage Used", value = "18.4 GB / 50 GB"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Auth Service", value = "Operational (JWT v2)"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Production URL", value = "secondme.ai"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Build Status", value = "Ready in 14s"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Daily Deployments", value = "18"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Edge Network", value = "22ms worldwide")
            )
            db.integrationDao().insertMetrics(metrics)

            // Seed Notifications with Exact Date & Time
            val notifications = listOf(
                NotificationEntity("n1", "Standup Reminder", "Daily engineering standup with team", "Aug 30, 2026 • 05:00 PM", NotificationType.REMINDER, NotificationState.UNREAD),
                NotificationEntity("n2", "DarkModeStudio Release v1.5.0", "Live GitHub account sync successfully deployed", "Aug 30, 2026 • 04:58 PM", NotificationType.BUILD_ALERT, NotificationState.UNREAD),
                NotificationEntity("n3", "Task deadline approaching", "“Verify Google OAuth” due at 07:30 PM", "Aug 30, 2026 • 04:30 PM", NotificationType.TASK_DEADLINE, NotificationState.READ),
                NotificationEntity("n4", "Claude 3.5 Sonnet quota alert", "Monthly token consumption reached 82%", "Aug 30, 2026 • 02:15 PM", NotificationType.AGENT_LIMIT, NotificationState.READ),
                NotificationEntity("n5", "Cloudflare edge health verified", "1.8M daily requests nominal across all zones", "Aug 30, 2026 • 12:00 PM", NotificationType.INCIDENT, NotificationState.READ)
            )
            db.notificationDao().insertNotifications(notifications)

            // Seed Reminders with Exact Date & Time
            val reminders = listOf(
                ReminderEntity("r1", "Daily Standup", "Aug 30, 2026 • 05:00 PM", isEnabled = true),
                ReminderEntity("r2", "Review Ghostcart KPIs", "Aug 30, 2026 • 07:00 PM", isEnabled = true),
                ReminderEntity("r3", "Weekly Architecture Planning", "Aug 31, 2026 • 10:00 AM", isEnabled = true)
            )
            db.reminderDao().insertReminders(reminders)

            // Seed Automation Rules
            val rules = listOf(
                AutomationRuleEntity("rule1", "GitHub Action Failure Alert", "GITHUB_WORKFLOW_FAILED", "github", "darkmodestudio", "SEND_NOTIFICATION", true, "WHEN GitHub Action fails THEN notify me"),
                AutomationRuleEntity("rule2", "Inactivity Watchdog", "PROJECT_INACTIVE_24H", null, null, "SEND_NOTIFICATION", true, "WHEN project has no activity for 24h THEN notify me"),
                AutomationRuleEntity("rule3", "Agent Quota Warning", "AGENT_QUOTA_HIGH", null, null, "SEND_NOTIFICATION", true, "WHEN agent quota > 80% THEN notify me"),
                AutomationRuleEntity("rule4", "Auto Mark Deployed Tasks", "DEPLOYMENT_SUCCESS", "cloudflare", null, "MARK_TASK_COMPLETE", true, "WHEN deployment succeeds THEN mark deployment task complete")
            )
            db.automationDao().insertRules(rules)

            // Seed App Settings
            db.settingsDao().insertOrUpdate(AppSettingsEntity())
        }
    }
}
