package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.NotificationDao
import com.darkmodestudio.commandcenter.core.database.dao.ReminderDao
import com.darkmodestudio.commandcenter.core.database.dao.SettingsDao
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationState
import com.darkmodestudio.commandcenter.core.database.entity.ReminderEntity
import com.darkmodestudio.commandcenter.core.model.NotificationToggleState
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ReminderItem
import com.darkmodestudio.commandcenter.core.model.UpdateNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class NotificationRepository(
    private val notificationDao: NotificationDao? = null,
    private val reminderDao: ReminderDao? = null,
    private val settingsDao: SettingsDao? = null
) {

    val notifications: Flow<List<UpdateNotification>> = notificationDao?.getNotificationsFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultNotifications)

    val reminders: Flow<List<ReminderItem>> = reminderDao?.getRemindersFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultReminders)

    val toggleStates: Flow<NotificationToggleState> = settingsDao?.getSettingsFlow()?.map { settings ->
        if (settings != null) {
            NotificationToggleState(
                pushReminders = settings.pushReminders,
                buildAlerts = settings.buildAlerts,
                taskDeadlines = settings.taskDeadlines,
                agentLimitWarnings = settings.agentLimitWarnings,
                platformIncidents = settings.platformIncidents,
                dailyBriefing = settings.dailyBriefing
            )
        } else {
            NotificationToggleState()
        }
    } ?: flowOf(NotificationToggleState())

    suspend fun createReminder(title: String, dueText: String): String {
        val id = "r_" + System.currentTimeMillis()
        val entity = ReminderEntity(
            id = id,
            title = title,
            dueText = dueText,
            isEnabled = true
        )
        reminderDao?.insertReminder(entity)
        return id
    }

    suspend fun toggleReminder(id: String, currentEnabled: Boolean) {
        reminderDao?.toggleReminder(id, !currentEnabled)
    }

    suspend fun deleteReminder(id: String) {
        reminderDao?.deleteReminder(id)
    }

    suspend fun createNotification(
        title: String,
        description: String,
        type: NotificationType,
        linkedType: String? = null,
        linkedId: String? = null
    ): String {
        val id = "notif_" + System.currentTimeMillis()
        val entity = NotificationEntity(
            id = id,
            title = title,
            description = description,
            timeAgo = "Just now",
            type = type,
            state = NotificationState.UNREAD,
            linkedType = linkedType,
            linkedId = linkedId
        )
        notificationDao?.insertNotification(entity)
        return id
    }

    suspend fun markAsRead(id: String) {
        notificationDao?.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao?.markAllAsRead()
    }

    suspend fun archiveNotification(id: String) {
        notificationDao?.archiveNotification(id)
    }

    suspend fun updateToggle(update: (NotificationToggleState) -> NotificationToggleState) {
        val current = settingsDao?.getSettings() ?: AppSettingsEntity()
        val currentToggle = NotificationToggleState(
            pushReminders = current.pushReminders,
            buildAlerts = current.buildAlerts,
            taskDeadlines = current.taskDeadlines,
            agentLimitWarnings = current.agentLimitWarnings,
            platformIncidents = current.platformIncidents,
            dailyBriefing = current.dailyBriefing
        )
        val newToggle = update(currentToggle)
        settingsDao?.insertOrUpdate(
            current.copy(
                pushReminders = newToggle.pushReminders,
                buildAlerts = newToggle.buildAlerts,
                taskDeadlines = newToggle.taskDeadlines,
                agentLimitWarnings = newToggle.agentLimitWarnings,
                platformIncidents = newToggle.platformIncidents,
                dailyBriefing = newToggle.dailyBriefing
            )
        )
    }

    companion object {
        val defaultNotifications = listOf(
            UpdateNotification("n1", "Standup reminder", "Daily standup with team in 19 minutes", "19m", NotificationType.REMINDER, isRead = false),
            UpdateNotification("n2", "GhostCart build #142 completed", "Production build deployed successfully to edge clusters", "42m", NotificationType.BUILD_ALERT, isRead = false),
            UpdateNotification("n3", "Task deadline approaching", "“Payment flow polish” due in 3 hours", "1h", NotificationType.TASK_DEADLINE, isRead = true),
            UpdateNotification("n4", "Claude usage limit at 82%", "You're nearing your monthly allocation ceiling", "3h", NotificationType.AGENT_LIMIT, isRead = true),
            UpdateNotification("n5", "Cloudflare incident resolved", "Performance restored across all North America regions", "5h", NotificationType.INCIDENT, isRead = true)
        )

        val defaultReminders = listOf(
            ReminderItem("r1", "Daily Standup", "09:30 AM", isEnabled = true),
            ReminderItem("r2", "Review GhostCart KPIs", "02:00 PM", isEnabled = true),
            ReminderItem("r3", "Weekly Architecture Planning", "05:00 PM", isEnabled = true)
        )
    }
}

private fun NotificationEntity.toDomain(): UpdateNotification {
    return UpdateNotification(
        id = id,
        title = title,
        description = description,
        timeAgo = timeAgo,
        type = type,
        isRead = state == NotificationState.READ
    )
}

private fun ReminderEntity.toDomain(): ReminderItem {
    return ReminderItem(
        id = id,
        title = title,
        dueText = dueText,
        isEnabled = isEnabled
    )
}
