package com.darkmodestudio.commandcenter.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.darkmodestudio.commandcenter.MainActivity
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationState
import com.darkmodestudio.commandcenter.core.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DmsNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders & Focus Blocks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Personal focus blocks, standups, and KPI reviews"
                enableVibration(true)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Build & CI Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "GitHub Actions workflow failures and deployment notices"
            }

            val incidentsChannel = NotificationChannel(
                CHANNEL_INCIDENTS,
                "Platform Incidents",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Degraded database or edge worker health alerts"
            }

            notificationManager.createNotificationChannels(listOf(remindersChannel, alertsChannel, incidentsChannel))
        }
    }

    fun postNotification(
        id: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_REMINDERS
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }

    fun scheduleExactReminder(reminderId: String, title: String, triggerEpochMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_id", reminderId)
            putExtra("reminder_title", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "dms_reminders"
        const val CHANNEL_ALERTS = "dms_alerts"
        const val CHANNEL_INCIDENTS = "dms_incidents"
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("reminder_title") ?: "Command Center Reminder"

        val dmsNotificationManager = DmsNotificationManager(context)
        dmsNotificationManager.postNotification(
            id = reminderId.hashCode(),
            title = title,
            message = "Scheduled focus block / standup notification"
        )

        // Ingest into SQLite Notifications inbox
        CoroutineScope(Dispatchers.IO).launch {
            val database = DmsDatabase.getInstance(context)
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = "notif_rem_" + System.currentTimeMillis(),
                    title = title,
                    description = "Scheduled reminder fired",
                    timeAgo = "Just now",
                    type = NotificationType.REMINDER,
                    state = NotificationState.UNREAD
                )
            )
        }
    }
}
