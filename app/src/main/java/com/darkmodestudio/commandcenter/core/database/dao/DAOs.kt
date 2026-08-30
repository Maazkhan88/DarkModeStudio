package com.darkmodestudio.commandcenter.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
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
import com.darkmodestudio.commandcenter.core.database.entity.RepositoryFileEntryEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow

data class ProjectWithDetails(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val milestones: List<ProjectMilestoneEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val blockers: List<ProjectBlockerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val activities: List<ProjectActivityEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tasks: List<TaskEntity>
)

data class IntegrationWithDetails(
    @Embedded val integration: IntegrationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "integrationId"
    )
    val metrics: List<IntegrationMetricEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "integrationId"
    )
    val incidents: List<IntegrationIncidentEntity>
)

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects")
    fun getProjectsWithDetailsFlow(): Flow<List<ProjectWithDetails>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    fun getProjectWithDetailsFlow(projectId: String): Flow<ProjectWithDetails?>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<ProjectMilestoneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockers(blockers: List<ProjectBlockerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ProjectActivityEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'PENDING' THEN 1 WHEN 'OVERDUE' THEN 2 WHEN 'BLOCKED' THEN 3 ELSE 4 END")
    fun getTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    fun getTasksByProjectFlow(projectId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus, completedAt: String?)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasksFlow(query: String): Flow<List<TaskEntity>>
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents")
    fun getAgentsFlow(): Flow<List<AgentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<AgentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Update
    suspend fun updateAgent(agent: AgentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageSnapshot(snapshot: AgentUsageSnapshotEntity)

    @Query("SELECT * FROM agent_usage_snapshots WHERE agentId = :agentId ORDER BY capturedAt DESC LIMIT 20")
    fun getUsageSnapshotsFlow(agentId: String): Flow<List<AgentUsageSnapshotEntity>>

    @Query("SELECT * FROM agent_usage_snapshots ORDER BY capturedAt DESC")
    fun getAllUsageSnapshotsFlow(): Flow<List<AgentUsageSnapshotEntity>>
}

@Dao
interface IntegrationDao {
    @Transaction
    @Query("SELECT * FROM integrations")
    fun getIntegrationsWithDetailsFlow(): Flow<List<IntegrationWithDetails>>

    @Query("SELECT * FROM integrations WHERE id = :id LIMIT 1")
    suspend fun getIntegrationById(id: String): IntegrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrations(integrations: List<IntegrationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegration(integration: IntegrationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<IntegrationMetricEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IntegrationIncidentEntity>)

    @Update
    suspend fun updateIntegration(integration: IntegrationEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE state != 'ARCHIVED' ORDER BY createdAtMillis DESC")
    fun getNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET state = 'READ' WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET state = 'READ' WHERE state = 'UNREAD'")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET state = 'ARCHIVED' WHERE id = :id")
    suspend fun archiveNotification(id: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isEnabled DESC, id ASC")
    fun getRemindersFlow(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleReminder(id: String, isEnabled: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_rules ORDER BY isEnabled DESC")
    fun getAutomationRulesFlow(): Flow<List<AutomationRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AutomationRuleEntity>)

    @Update
    suspend fun updateRule(rule: AutomationRuleEntity)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: AutomationExecutionEntity)

    @Query("SELECT COUNT(*) FROM automation_executions WHERE triggeredAt >= :sinceTimestamp")
    suspend fun getExecutionCountSince(sinceTimestamp: Long): Int
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)
}

@Dao
interface RepositoryFileDao {
    @Query("SELECT * FROM repository_file_entries WHERE repositoryFullName = :repoFullName AND path = :path ORDER BY CASE type WHEN 'dir' THEN 1 ELSE 2 END, name ASC")
    fun getFilesFlow(repoFullName: String, path: String): Flow<List<RepositoryFileEntryEntity>>

    @Query("SELECT * FROM repository_file_entries WHERE repositoryFullName = :repoFullName AND path = :path ORDER BY CASE type WHEN 'dir' THEN 1 ELSE 2 END, name ASC")
    suspend fun getFiles(repoFullName: String, path: String): List<RepositoryFileEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<RepositoryFileEntryEntity>)

    @Query("DELETE FROM repository_file_entries WHERE repositoryFullName = :repoFullName AND path = :path")
    suspend fun deleteFilesForPath(repoFullName: String, path: String)

    @Query("DELETE FROM repository_file_entries WHERE repositoryFullName = :repoFullName")
    suspend fun deleteAllForRepo(repoFullName: String)
}
