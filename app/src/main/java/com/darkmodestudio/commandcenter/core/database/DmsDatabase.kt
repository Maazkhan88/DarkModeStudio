package com.darkmodestudio.commandcenter.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
        IntegrationEntity::class,
        IntegrationMetricEntity::class,
        IntegrationIncidentEntity::class,
        NotificationEntity::class,
        ReminderEntity::class,
        AutomationRuleEntity::class,
        AutomationExecutionEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
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
                    "dark_mode_studio.db"
                )
                    .addCallback(DatabaseSeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedInitialData(database)
                }
            }
        }

        private suspend fun seedInitialData(db: DmsDatabase) {
            // Seed Projects
            val projects = listOf(
                ProjectEntity(
                    id = "secondme",
                    name = "SecondMe",
                    description = "Personal AI memory & continuous persona clone",
                    iconTag = "SM",
                    status = ProjectStatus.IN_PROGRESS,
                    isMvp = true,
                    owner = "Founder",
                    createdAt = "Aug 01, 2026",
                    dueDate = "Oct 01, 2026",
                    nextMilestone = "User Memory Sync",
                    manualProgressOverride = 0.54f,
                    planningWeight = 0.15f,
                    developmentWeight = 0.45f,
                    testingWeight = 0.20f,
                    deploymentWeight = 0.20f,
                    lastUpdate = "12m ago"
                ),
                ProjectEntity(
                    id = "ghostcart",
                    name = "GhostCart",
                    description = "Ultra-fast headless commerce checkout system",
                    iconTag = "GC",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = false,
                    owner = "Founder",
                    createdAt = "Jul 15, 2026",
                    dueDate = "Sep 15, 2026",
                    nextMilestone = "Production Load Test",
                    manualProgressOverride = 0.78f,
                    lastUpdate = "4m ago"
                ),
                ProjectEntity(
                    id = "proptree",
                    name = "Proptree",
                    description = "Real estate intelligence graph and deal radar",
                    iconTag = "PT",
                    status = ProjectStatus.WAITING,
                    isMvp = false,
                    owner = "Founder",
                    createdAt = "Aug 10, 2026",
                    dueDate = "Nov 10, 2026",
                    nextMilestone = "Data Schema Ingestion",
                    manualProgressOverride = 0.32f,
                    lastUpdate = "2h ago"
                ),
                ProjectEntity(
                    id = "agstudio",
                    name = "AG Studio",
                    description = "Autonomous coding agent IDE & swarm orchestrator",
                    iconTag = "AG",
                    status = ProjectStatus.ON_TRACK,
                    isMvp = false,
                    owner = "Founder",
                    createdAt = "Jun 01, 2026",
                    dueDate = "Sep 05, 2026",
                    nextMilestone = "Release v2.0",
                    manualProgressOverride = 0.91f,
                    lastUpdate = "1m ago"
                ),
                ProjectEntity(
                    id = "pioneer",
                    name = "Pioneer",
                    description = "Low-latency streaming audio bridge & DSP node",
                    iconTag = "PN",
                    status = ProjectStatus.BLOCKED,
                    isMvp = false,
                    owner = "Founder",
                    createdAt = "Aug 20, 2026",
                    dueDate = "Dec 01, 2026",
                    nextMilestone = "Core DSP Pipeline",
                    manualProgressOverride = 0.15f,
                    lastUpdate = "5h ago"
                )
            )
            db.projectDao().insertProjects(projects)

            // Seed SecondMe Milestones
            val smMilestones = listOf(
                ProjectMilestoneEntity("sm_m1", "secondme", "Architecture", isCompleted = true, isActive = false, date = "Aug 10", sortOrder = 0),
                ProjectMilestoneEntity("sm_m2", "secondme", "Memory Engine", isCompleted = true, isActive = false, date = "Aug 22", sortOrder = 1),
                ProjectMilestoneEntity("sm_m3", "secondme", "Sync Layer", isCompleted = false, isActive = true, date = "Sep 05", sortOrder = 2),
                ProjectMilestoneEntity("sm_m4", "secondme", "Staging v1.0", isCompleted = false, isActive = false, date = "Sep 20", sortOrder = 3)
            )
            db.projectDao().insertMilestones(smMilestones)

            // Seed Blockers
            val blockers = listOf(
                ProjectBlockerEntity("b1", "secondme", "Blocked on Cloudflare Workers AI rate-limit tier elevation", "High", "6 hours"),
                ProjectBlockerEntity("b2", "pioneer", "Native C++ bridge crash on Android 15 ARM64", "High", "2 days")
            )
            db.projectDao().insertBlockers(blockers)

            // Seed Activities
            val activities = listOf(
                ProjectActivityEntity("a1", "secondme", "feat: add user memory sync", "Codex", "a1b2c3d", "18m ago"),
                ProjectActivityEntity("a2", "secondme", "Deploy: staging v0.4.2", "Claude", "deployed", "1h ago"),
                ProjectActivityEntity("a3", "secondme", "fix: resolve auth edge case", "Antigravity", "d4e5f6a", "3h ago")
            )
            db.projectDao().insertActivities(activities)

            // Seed Tasks
            val tasks = listOf(
                TaskEntity("t1", "secondme", "SecondMe", "Review PR #342 — Auth token rotation", "Verify biometric fallback & refresh token lifecycle", TaskStatus.PENDING, TaskPriority.HIGH, "Codex", "09:00"),
                TaskEntity("t2", "ghostcart", "GhostCart", "Push build to Internal Track", "Google Play Console release candidate v1.0.0-rc3", TaskStatus.PENDING, TaskPriority.HIGH, "Claude", "11:00"),
                TaskEntity("t3", "agstudio", "AG Studio", "Confirm deployment & verify telemetry", "Check Cloudflare Worker edge logs and Crashlytics", TaskStatus.PENDING, TaskPriority.MEDIUM, "Antigravity", "16:30"),
                TaskEntity("t4", "secondme", "SecondMe", "Implement offline SQLite vector index", "Optimize cosine similarity query latency below 12ms", TaskStatus.DONE, TaskPriority.HIGH, "Codex", "Yesterday", completedAt = "Yesterday"),
                TaskEntity("t5", "proptree", "Proptree", "Resolve Supabase connection pool exhaustion", "Add PgBouncer transaction mode endpoint", TaskStatus.BLOCKED, TaskPriority.HIGH, "Claude", "Today"),
                TaskEntity("t6", "pioneer", "Pioneer", "Fix ARM64 native memory leak in audio loop", "DSP ring buffer pointer misalignment", TaskStatus.OVERDUE, TaskPriority.HIGH, "Codex", "2d overdue")
            )
            db.taskDao().insertTasks(tasks)

            // Seed Agents
            val agents = listOf(
                AgentEntity("codex", "Codex", AgentProvider.OPENAI, "Pro", "Fast", 225, 500, 2150, 5000, 48, 100, "Refactor auth module and add unit tests", "In Progress • 68%", 0.68f),
                AgentEntity("claude", "Claude", AgentProvider.ANTHROPIC, "Opus", "Pro", 160, 600, 4320, 10000, 112, 300, "Synthesize GhostCart API schema and edge endpoints", "Active • 82%", 0.82f),
                AgentEntity("antigravity", "Antigravity", AgentProvider.ANTIGRAVITY, "Swarm", "Max", 94, 400, 2150, 5000, 53, 200, "Orchestrate multi-agent build pipeline & QA review", "Ready • 41%", 0.41f),
                AgentEntity("custom_agent", "Custom Agent", AgentProvider.CUSTOM, "Local", "Manual", 0, 100, 0, 1000, 0, 50, "Idle — Local manual sync adapter connected", "Standby • 0%", 0.0f)
            )
            db.agentDao().insertAgents(agents)

            // Seed Integrations
            val integrations = listOf(
                IntegrationEntity("github", "GitHub", "Code & CI/CD", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "All CI Actions Passing"),
                IntegrationEntity("cloudflare", "Cloudflare", "Edge & Infrastructure", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "1.4M req • 0.02% err"),
                IntegrationEntity("firebase", "Firebase", "Mobile & Crashlytics", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "99.94% Crash-Free"),
                IntegrationEntity("play_console", "Google Play Console", "Distribution", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "Internal Track v1.0.0-rc2"),
                IntegrationEntity("apple_dev", "Apple Developer", "Distribution", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "TestFlight v1.0 (42)"),
                IntegrationEntity("supabase", "Supabase", "Database & Auth", isConnected = true, health = IntegrationHealth.DEGRADED, primaryMetric = "Connection Pool High"),
                IntegrationEntity("vercel", "Vercel", "Frontend & Edge", isConnected = true, health = IntegrationHealth.OPERATIONAL, primaryMetric = "Deploy: secondme-web")
            )
            db.integrationDao().insertIntegrations(integrations)

            val metrics = listOf(
                IntegrationMetricEntity(integrationId = "github", label = "Primary Repo", value = "darkmodestudio/core"),
                IntegrationMetricEntity(integrationId = "github", label = "Last Push", value = "4m ago (main)"),
                IntegrationMetricEntity(integrationId = "github", label = "Open PRs", value = "3 review required"),
                IntegrationMetricEntity(integrationId = "github", label = "Workflows", value = "12 passing / 0 failing"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Daily Requests", value = "1,420,890"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Error Rate", value = "0.02% (nominal)"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Cache Hit Ratio", value = "94.8%"),
                IntegrationMetricEntity(integrationId = "cloudflare", label = "Workers Status", value = "14 workers active"),
                IntegrationMetricEntity(integrationId = "firebase", label = "Latest Build", value = "Release #28"),
                IntegrationMetricEntity(integrationId = "firebase", label = "Crash-Free Rate", value = "99.94% sessions"),
                IntegrationMetricEntity(integrationId = "firebase", label = "Auth Service", value = "Operational"),
                IntegrationMetricEntity(integrationId = "firebase", label = "FCM Messaging", value = "Healthy"),
                IntegrationMetricEntity(integrationId = "play_console", label = "Active Track", value = "Internal Testing"),
                IntegrationMetricEntity(integrationId = "play_console", label = "Version", value = "1.0.0-rc2 (104)"),
                IntegrationMetricEntity(integrationId = "play_console", label = "Crash Rate", value = "0.01% ANR/Crash"),
                IntegrationMetricEntity(integrationId = "play_console", label = "Rating", value = "4.9 ★ (Internal QA)"),
                IntegrationMetricEntity(integrationId = "apple_dev", label = "TestFlight Build", value = "v1.0 (Build 42)"),
                IntegrationMetricEntity(integrationId = "apple_dev", label = "External Testers", value = "14 Active"),
                IntegrationMetricEntity(integrationId = "apple_dev", label = "App Store Status", value = "Pending Binary Upload"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Database Latency", value = "42ms"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Pool Usage", value = "88% threshold alert"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Storage Used", value = "18.4 GB / 50 GB"),
                IntegrationMetricEntity(integrationId = "supabase", label = "Auth Endpoints", value = "100% Available"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Production URL", value = "secondme-web.app"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Build Status", value = "Ready in 18s"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Daily Deployments", value = "14"),
                IntegrationMetricEntity(integrationId = "vercel", label = "Edge Latency", value = "28ms avg")
            )
            db.integrationDao().insertMetrics(metrics)

            val incidents = listOf(
                IntegrationIncidentEntity("inc1", "supabase", "Connection pool above 85% on replica 02", "Threshold alert", "45s ago", false)
            )
            db.integrationDao().insertIncidents(incidents)

            // Seed Notifications
            val notifications = listOf(
                NotificationEntity("n1", "Standup reminder", "Daily standup with team in 19 minutes", "19m", NotificationType.REMINDER, NotificationState.UNREAD),
                NotificationEntity("n2", "GhostCart build #142 completed", "Production build deployed successfully to edge clusters", "42m", NotificationType.BUILD_ALERT, NotificationState.UNREAD),
                NotificationEntity("n3", "Task deadline approaching", "“Payment flow polish” due in 3 hours", "1h", NotificationType.TASK_DEADLINE, NotificationState.READ),
                NotificationEntity("n4", "Claude usage limit at 82%", "You're nearing your monthly allocation ceiling", "3h", NotificationType.AGENT_LIMIT, NotificationState.READ),
                NotificationEntity("n5", "Cloudflare incident resolved", "Performance restored across all North America regions", "5h", NotificationType.INCIDENT, NotificationState.READ)
            )
            db.notificationDao().insertNotifications(notifications)

            // Seed Reminders
            val reminders = listOf(
                ReminderEntity("r1", "Daily Standup", "09:30 AM", isEnabled = true),
                ReminderEntity("r2", "Review GhostCart KPIs", "02:00 PM", isEnabled = true),
                ReminderEntity("r3", "Weekly Architecture Planning", "05:00 PM", isEnabled = true)
            )
            db.reminderDao().insertReminders(reminders)

            // Seed Automations
            val rules = listOf(
                AutomationRuleEntity("rule1", "GitHub Action Failure Alert", "GITHUB_WORKFLOW_FAILED", "github", "ghostcart", "SEND_NOTIFICATION", true, "WHEN GitHub Action fails THEN notify me"),
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
