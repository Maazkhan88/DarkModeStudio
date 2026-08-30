package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.model.NotificationToggleState
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ReminderItem
import com.darkmodestudio.commandcenter.core.model.UpdateNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationRepository {

    private val _notifications = MutableStateFlow(
        listOf(
            UpdateNotification(
                id = "n1",
                title = "Standup reminder",
                description = "Daily standup with team in 19 minutes",
                timeAgo = "19m",
                type = NotificationType.REMINDER,
                isRead = false
            ),
            UpdateNotification(
                id = "n2",
                title = "GhostCart build #142 completed",
                description = "Production build deployed successfully to edge clusters",
                timeAgo = "42m",
                type = NotificationType.BUILD_ALERT,
                isRead = false
            ),
            UpdateNotification(
                id = "n3",
                title = "Task deadline approaching",
                description = "“Payment flow polish” due in 3 hours",
                timeAgo = "1h",
                type = NotificationType.TASK_DEADLINE,
                isRead = true
            ),
            UpdateNotification(
                id = "n4",
                title = "Claude usage limit at 82%",
                description = "You're nearing your monthly allocation ceiling",
                timeAgo = "3h",
                type = NotificationType.AGENT_LIMIT,
                isRead = true
            ),
            UpdateNotification(
                id = "n5",
                title = "Cloudflare incident resolved",
                description = "Performance restored across all North America regions",
                timeAgo = "5h",
                type = NotificationType.INCIDENT,
                isRead = true
            )
        )
    )

    private val _reminders = MutableStateFlow(
        listOf(
            ReminderItem(id = "r1", title = "Daily Standup", dueText = "09:30 AM", isEnabled = true),
            ReminderItem(id = "r2", title = "Review GhostCart KPIs", dueText = "02:00 PM", isEnabled = true),
            ReminderItem(id = "r3", title = "Weekly Architecture Planning", dueText = "05:00 PM", isEnabled = true)
        )
    )

    private val _toggleStates = MutableStateFlow(NotificationToggleState())

    val notifications: Flow<List<UpdateNotification>> = _notifications.asStateFlow()
    val reminders: Flow<List<ReminderItem>> = _reminders.asStateFlow()
    val toggleStates: Flow<NotificationToggleState> = _toggleStates.asStateFlow()

    fun toggleReminder(id: String) {
        _reminders.update { list ->
            list.map { if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it }
        }
    }

    fun updateToggle(update: (NotificationToggleState) -> NotificationToggleState) {
        _toggleStates.update(update)
    }
}
