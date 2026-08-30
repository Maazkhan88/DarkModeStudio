package com.darkmodestudio.commandcenter.core.model

enum class NotificationType(val displayName: String) {
    REMINDER("Reminder"),
    BUILD_ALERT("Build Alert"),
    TASK_DEADLINE("Task Deadline"),
    AGENT_LIMIT("Agent Limit"),
    INCIDENT("Incident")
}

data class UpdateNotification(
    val id: String,
    val title: String,
    val description: String,
    val timeAgo: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

data class ReminderItem(
    val id: String,
    val title: String,
    val dueText: String,
    val isEnabled: Boolean = true
)
