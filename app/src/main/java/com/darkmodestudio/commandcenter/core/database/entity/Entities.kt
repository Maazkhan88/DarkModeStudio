package com.darkmodestudio.commandcenter.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconTag: String,
    val status: ProjectStatus,
    val isMvp: Boolean,
    val owner: String,
    val createdAt: String,
    val dueDate: String,
    val nextMilestone: String,
    val manualProgressOverride: Float? = null,
    val planningWeight: Float = 0.15f,
    val developmentWeight: Float = 0.45f,
    val testingWeight: Float = 0.20f,
    val deploymentWeight: Float = 0.20f,
    val lastUpdate: String = "Just now",
    val repositoryFullName: String? = null,
    val repositoryDefaultBranch: String? = null
)

@Entity(
    tableName = "project_milestones",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectMilestoneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val isCompleted: Boolean,
    val isActive: Boolean,
    val date: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "project_blockers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectBlockerEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val description: String,
    val severity: String = "High",
    val duration: String
)

@Entity(
    tableName = "project_activities",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class ProjectActivityEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val author: String,
    val hash: String? = null,
    val timestamp: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["projectId", "status"]),
        Index(value = ["status"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val assignedAgent: String? = null,
    val dueTime: String,
    val createdAt: String = "Today",
    val completedAt: String? = null
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val provider: AgentProvider,
    val mode: String = "Pro",
    val speed: String = "Fast",
    val runsUsed: Int = 0,
    val runsTotal: Int = 500,
    val messagesUsed: Int = 0,
    val messagesTotal: Int = 5000,
    val tasksUsed: Int = 0,
    val tasksTotal: Int = 100,
    val currentTask: String = "Ready for execution",
    val statusText: String = "Standby • 0%",
    val usagePercentage: Float = 0.0f
)

@Entity(
    tableName = "agent_usage_snapshots",
    indices = [Index(value = ["agentId"])]
)
data class AgentUsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: String,
    val dataSource: String, // "API", "ADMIN_API", "LOCAL_TELEMETRY", "CLI", "MANUAL", "UNAVAILABLE"
    val requestsUsed: Int,
    val requestsLimit: Int,
    val messagesUsed: Int,
    val messagesLimit: Int,
    val tokensUsed: Long,
    val resetAt: String? = null,
    val capturedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "agent_activities",
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["agentId"])]
)
data class AgentActivityEntity(
    @PrimaryKey val id: String,
    val agentId: String,
    val title: String,
    val taskReference: String,
    val timestamp: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "integrations")
data class IntegrationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val isConnected: Boolean = true,
    val health: IntegrationHealth = IntegrationHealth.OPERATIONAL,
    val lastSync: String = "Just now",
    val lastSuccessfulSync: String? = null,
    val lastError: String? = null,
    val primaryMetric: String
)

@Entity(
    tableName = "integration_metrics",
    foreignKeys = [
        ForeignKey(
            entity = IntegrationEntity::class,
            parentColumns = ["id"],
            childColumns = ["integrationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["integrationId"])]
)
data class IntegrationMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val integrationId: String,
    val label: String,
    val value: String
)

@Entity(
    tableName = "integration_incidents",
    foreignKeys = [
        ForeignKey(
            entity = IntegrationEntity::class,
            parentColumns = ["id"],
            childColumns = ["integrationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["integrationId"])]
)
data class IntegrationIncidentEntity(
    @PrimaryKey val id: String,
    val integrationId: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val isResolved: Boolean = false
)

enum class NotificationState {
    UNREAD,
    READ,
    ARCHIVED
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val type: NotificationType,
    val state: NotificationState = NotificationState.UNREAD,
    val linkedType: String? = null,
    val linkedId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueText: String,
    val scheduledEpochMillis: Long = 0,
    val isEnabled: Boolean = true,
    val repeatRule: String? = null
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerType: String,
    val providerId: String? = null,
    val projectId: String? = null,
    val actionType: String,
    val isEnabled: Boolean = true,
    val humanReadableText: String
)

@Entity(
    tableName = "automation_executions",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ruleId"])]
)
data class AutomationExecutionEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val result: String,
    val message: String,
    val sourceEventId: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val biometricLock: Boolean = true,
    val syncFrequency: String = "15 minutes",
    val dailyBriefing: Boolean = true,
    val pushReminders: Boolean = true,
    val buildAlerts: Boolean = true,
    val taskDeadlines: Boolean = true,
    val agentLimitWarnings: Boolean = true,
    val platformIncidents: Boolean = true,
    val schemaSeedVersion: Int = 4
)

@Entity(
    tableName = "repository_file_entries",
    indices = [Index(value = ["repositoryFullName", "path"])]
)
data class RepositoryFileEntryEntity(
    @PrimaryKey val id: String, // "$repositoryFullName:$path:$name"
    val repositoryFullName: String,
    val path: String, // parent folder path e.g. "" for root or "app/src"
    val name: String,
    val fullPath: String,
    val type: String, // "file" or "dir"
    val size: Long = 0,
    val sha: String = "",
    val downloadUrl: String? = null,
    val lastCached: Long = System.currentTimeMillis()
)
