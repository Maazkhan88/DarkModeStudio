package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.model.Task
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TaskRepository {

    private val _tasks = MutableStateFlow(
        listOf(
            Task(
                id = "t1",
                title = "Review PR #342 — Auth token rotation",
                description = "Verify biometric fallback & refresh token lifecycle",
                projectId = "secondme",
                projectName = "SecondMe",
                status = TaskStatus.PENDING,
                priority = TaskPriority.HIGH,
                assignedAgent = "Codex",
                dueTime = "09:00"
            ),
            Task(
                id = "t2",
                title = "Push build to Internal Track",
                description = "Google Play Console release candidate v1.0.0-rc3",
                projectId = "ghostcart",
                projectName = "GhostCart",
                status = TaskStatus.PENDING,
                priority = TaskPriority.HIGH,
                assignedAgent = "Claude",
                dueTime = "11:00"
            ),
            Task(
                id = "t3",
                title = "Confirm deployment & verify telemetry",
                description = "Check Cloudflare Worker edge logs and Crashlytics",
                projectId = "agstudio",
                projectName = "AG Studio",
                status = TaskStatus.PENDING,
                priority = TaskPriority.MEDIUM,
                assignedAgent = "Antigravity",
                dueTime = "16:30"
            ),
            Task(
                id = "t4",
                title = "Implement offline SQLite vector index",
                description = "Optimize cosine similarity query latency below 12ms",
                projectId = "secondme",
                projectName = "SecondMe",
                status = TaskStatus.DONE,
                priority = TaskPriority.HIGH,
                assignedAgent = "Codex",
                dueTime = "Yesterday"
            ),
            Task(
                id = "t5",
                title = "Resolve Supabase connection pool exhaustion",
                description = "Add PgBouncer transaction mode endpoint",
                projectId = "proptree",
                projectName = "Proptree",
                status = TaskStatus.BLOCKED,
                priority = TaskPriority.HIGH,
                assignedAgent = "Claude",
                dueTime = "Today"
            ),
            Task(
                id = "t6",
                title = "Fix ARM64 native memory leak in audio loop",
                description = "DSP ring buffer pointer misalignment",
                projectId = "pioneer",
                projectName = "Pioneer",
                status = TaskStatus.OVERDUE,
                priority = TaskPriority.HIGH,
                assignedAgent = "Codex",
                dueTime = "2d overdue"
            )
        )
    )

    val tasks: Flow<List<Task>> = _tasks.asStateFlow()

    fun toggleTask(taskId: String) {
        _tasks.update { currentList ->
            currentList.map { task ->
                if (task.id == taskId) {
                    val nextStatus = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
                    task.copy(status = nextStatus)
                } else {
                    task
                }
            }
        }
    }
}
