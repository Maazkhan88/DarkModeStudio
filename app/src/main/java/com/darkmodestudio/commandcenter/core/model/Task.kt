package com.darkmodestudio.commandcenter.core.model

enum class TaskStatus(val displayName: String) {
    DONE("Done"),
    PENDING("Pending"),
    BLOCKED("Blocked"),
    OVERDUE("Overdue")
}

enum class TaskPriority(val displayName: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val projectId: String,
    val projectName: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val assignedAgent: String? = null,
    val dueTime: String,
    val createdAt: String = "Today",
    val completedAt: String? = null
)
