package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.TaskDao
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import com.darkmodestudio.commandcenter.core.model.Task
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao? = null) {

    val tasks: Flow<List<Task>> = taskDao?.getTasksFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultTasks)

    fun searchTasksFlow(query: String): Flow<List<Task>> {
        return taskDao?.searchTasksFlow(query)?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(defaultTasks.filter { it.title.contains(query, ignoreCase = true) })
    }

    suspend fun createTask(
        title: String,
        description: String?,
        projectId: String,
        projectName: String,
        priority: TaskPriority,
        assignedAgent: String?,
        dueTime: String
    ): String {
        val id = "t_" + System.currentTimeMillis()
        val entity = TaskEntity(
            id = id,
            projectId = projectId,
            projectName = projectName,
            title = title,
            description = description,
            status = TaskStatus.PENDING,
            priority = priority,
            assignedAgent = assignedAgent,
            dueTime = dueTime,
            createdAt = "Today"
        )
        taskDao?.insertTask(entity)
        return id
    }

    suspend fun updateTask(task: Task) {
        val entity = TaskEntity(
            id = task.id,
            projectId = task.projectId,
            projectName = task.projectName,
            title = task.title,
            description = task.description,
            status = task.status,
            priority = task.priority,
            assignedAgent = task.assignedAgent,
            dueTime = task.dueTime,
            createdAt = task.createdAt,
            completedAt = task.completedAt
        )
        taskDao?.updateTask(entity)
    }

    suspend fun toggleTask(taskId: String, currentStatus: TaskStatus = TaskStatus.PENDING) {
        val nextStatus = if (currentStatus == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
        val completedAt = if (nextStatus == TaskStatus.DONE) "Just now" else null
        taskDao?.updateTaskStatus(taskId, nextStatus, completedAt)
    }

    suspend fun deleteTask(taskId: String) {
        taskDao?.deleteTask(taskId)
    }

    companion object {
        val defaultTasks = listOf(
            Task("t1", "Review PR #342 — Auth token rotation", "Verify biometric fallback & refresh token lifecycle", "secondme", "SecondMe", TaskStatus.PENDING, TaskPriority.HIGH, "Codex", "09:00", "Today"),
            Task("t2", "Push build to Internal Track", "Google Play Console release candidate v1.0.0-rc3", "ghostcart", "GhostCart", TaskStatus.PENDING, TaskPriority.HIGH, "Claude", "11:00", "Today"),
            Task("t3", "Confirm deployment & verify telemetry", "Check Cloudflare Worker edge logs and Crashlytics", "agstudio", "AG Studio", TaskStatus.PENDING, TaskPriority.MEDIUM, "Antigravity", "16:30", "Today"),
            Task("t4", "Implement offline SQLite vector index", "Optimize cosine similarity query latency below 12ms", "secondme", "SecondMe", TaskStatus.DONE, TaskPriority.HIGH, "Codex", "Yesterday", "Yesterday", "Yesterday"),
            Task("t5", "Resolve Supabase connection pool exhaustion", "Add PgBouncer transaction mode endpoint", "proptree", "Proptree", TaskStatus.BLOCKED, TaskPriority.HIGH, "Claude", "Today", "Today"),
            Task("t6", "Fix ARM64 native memory leak in audio loop", "DSP ring buffer pointer misalignment", "pioneer", "Pioneer", TaskStatus.OVERDUE, TaskPriority.HIGH, "Codex", "2d overdue", "2d ago")
        )
    }
}

private fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        projectId = projectId,
        projectName = projectName,
        status = status,
        priority = priority,
        assignedAgent = assignedAgent,
        dueTime = dueTime,
        createdAt = createdAt,
        completedAt = completedAt
    )
}
