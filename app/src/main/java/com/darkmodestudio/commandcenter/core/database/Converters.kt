package com.darkmodestudio.commandcenter.core.database

import androidx.room.TypeConverter
import com.darkmodestudio.commandcenter.core.database.entity.NotificationState
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = enumValueOf(value)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = enumValueOf(value)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = enumValueOf(value)

    @TypeConverter
    fun fromAgentProvider(value: AgentProvider): String = value.name

    @TypeConverter
    fun toAgentProvider(value: String): AgentProvider = enumValueOf(value)

    @TypeConverter
    fun fromIntegrationHealth(value: IntegrationHealth): String = value.name

    @TypeConverter
    fun toIntegrationHealth(value: String): IntegrationHealth = enumValueOf(value)

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = enumValueOf(value)

    @TypeConverter
    fun fromNotificationState(value: NotificationState): String = value.name

    @TypeConverter
    fun toNotificationState(value: String): NotificationState = enumValueOf(value)
}
