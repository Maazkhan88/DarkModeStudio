package com.darkmodestudio.commandcenter.feature.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsNode
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryOutlineButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsToggle
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.NotificationType
import com.darkmodestudio.commandcenter.core.model.ReminderItem
import com.darkmodestudio.commandcenter.core.model.UpdateNotification

@Composable
fun UpdatesScreen(
    notificationRepository: NotificationRepository,
    onAvatarClick: () -> Unit
) {
    val notifications by notificationRepository.notifications.collectAsState(initial = emptyList())
    val reminders by notificationRepository.reminders.collectAsState(initial = emptyList())
    val toggleStates by notificationRepository.toggleStates.collectAsState(initial = com.darkmodestudio.commandcenter.core.model.NotificationToggleState())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "updates",
            onAvatarClick = onAvatarClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Subtitle + Top Action Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Updates",
                            style = DmsTheme.typography.displayL
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stay informed. Act faster.",
                            style = DmsTheme.typography.bodySmall.copy(color = DmsColors.White64)
                        )
                    }

                    DmsSecondaryOutlineButton(
                        text = "Settings",
                        onClick = { /* Open preferences */ },
                        height = 36.dp
                    )
                }
            }

            // NOTIFICATION OVERVIEW (5 compact cards)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val summaryCards = listOf(
                        Triple("Reminders", "3", Icons.Outlined.Schedule),
                        Triple("Build Alerts", "4", Icons.Outlined.Build),
                        Triple("Task Deadlines", "2", Icons.Outlined.TaskAlt),
                        Triple("Agent Limits", "1", Icons.Outlined.Memory),
                        Triple("Incidents", "0", Icons.Outlined.WarningAmber)
                    )

                    items(summaryCards) { (label, count, icon) ->
                        CompactOverviewCard(label = label, count = count, icon = icon)
                    }
                }
            }

            // RECENT UPDATES FEED
            item {
                Text(
                    text = "Recent Updates",
                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(notifications) { item ->
                RecentUpdateCard(item = item)
            }

            // SCHEDULED REMINDERS
            item {
                Text(
                    text = "Scheduled Reminders",
                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(reminders) { reminder ->
                ReminderRowCard(
                    reminder = reminder,
                    onToggle = { notificationRepository.toggleReminder(reminder.id) }
                )
            }

            // NOTIFICATION CATEGORIES (Custom Monochrome Toggles)
            item {
                Text(
                    text = "Notification Categories",
                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR18,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow(
                            title = "Push Reminders",
                            subtitle = "Standups, KPI reviews, and focus timers",
                            checked = toggleStates.pushReminders,
                            onCheckedChange = { checked ->
                                notificationRepository.updateToggle { it.copy(pushReminders = checked) }
                            }
                        )
                        Divider()
                        ToggleRow(
                            title = "Build Alerts",
                            subtitle = "GitHub Actions, CI completions, and Play releases",
                            checked = toggleStates.buildAlerts,
                            onCheckedChange = { checked ->
                                notificationRepository.updateToggle { it.copy(buildAlerts = checked) }
                            }
                        )
                        Divider()
                        ToggleRow(
                            title = "Task Deadlines",
                            subtitle = "Upcoming task and milestone due date warnings",
                            checked = toggleStates.taskDeadlines,
                            onCheckedChange = { checked ->
                                notificationRepository.updateToggle { it.copy(taskDeadlines = checked) }
                            }
                        )
                        Divider()
                        ToggleRow(
                            title = "Agent Limit Warnings",
                            subtitle = "Quota alerts when quota consumption exceeds 80%",
                            checked = toggleStates.agentLimitWarnings,
                            onCheckedChange = { checked ->
                                notificationRepository.updateToggle { it.copy(agentLimitWarnings = checked) }
                            }
                        )
                        Divider()
                        ToggleRow(
                            title = "Platform Incidents",
                            subtitle = "Degraded database or edge worker latency spikes",
                            checked = toggleStates.platformIncidents,
                            onCheckedChange = { checked ->
                                notificationRepository.updateToggle { it.copy(platformIncidents = checked) }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun CompactOverviewCard(
    label: String,
    count: String,
    icon: ImageVector
) {
    DmsCard(
        modifier = Modifier
            .size(width = 96.dp, height = 76.dp),
        shape = DmsRadii.ShapeR14,
        backgroundColor = DmsColors.Surface01,
        padding = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DmsColors.White80,
                    modifier = Modifier.size(16.dp)
                )
                DmsNode(style = NodeStyle.SOLID, size = 4.dp, color = DmsColors.White)
            }

            Column {
                Text(
                    text = count,
                    style = DmsTheme.typography.h4.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DmsColors.White
                    )
                )
                Text(
                    text = label,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White48
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecentUpdateCard(item: UpdateNotification) {
    DmsCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DmsRadii.ShapeR14,
        backgroundColor = DmsColors.Surface01,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(DmsColors.White)
                    )
                } else {
                    Spacer(modifier = Modifier.width(5.dp))
                }

                Column {
                    Text(
                        text = item.title,
                        style = DmsTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DmsColors.White
                        )
                    )
                    Text(
                        text = item.description,
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 10.5.sp,
                            color = DmsColors.White64
                        ),
                        maxLines = 1
                    )
                }
            }

            Text(
                text = item.timeAgo,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White32
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ReminderRowCard(
    reminder: ReminderItem,
    onToggle: () -> Unit
) {
    DmsCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DmsRadii.ShapeR14,
        backgroundColor = DmsColors.Surface01,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = DmsColors.White80,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = reminder.title,
                        style = DmsTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (reminder.isEnabled) DmsColors.White else DmsColors.White48
                        )
                    )
                    Text(
                        text = reminder.dueText,
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 10.sp,
                            color = DmsColors.White48
                        )
                    )
                }
            }

            DmsToggle(
                checked = reminder.isEnabled,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DmsTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = DmsColors.White
                )
            )
            Text(
                text = subtitle,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White48
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        DmsToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DmsColors.White10)
    )
}
