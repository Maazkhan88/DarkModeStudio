package com.darkmodestudio.commandcenter.feature.execution

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRing
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsVerticalTimeRail
import com.darkmodestudio.commandcenter.core.designsystem.component.TimeRailItem
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Task
import com.darkmodestudio.commandcenter.core.model.TaskStatus

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ExecutionScreen(
    taskRepository: TaskRepository,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onAddTaskClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by taskRepository.tasks.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All Tasks") }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "My Tasks" -> tasks.filter { it.assignedAgent == "Codex" || it.assignedAgent == "Antigravity" }
            "Watchlist" -> tasks.filter { it.status == TaskStatus.BLOCKED || it.status == TaskStatus.OVERDUE }
            else -> tasks
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Top Bar
            DmsTopBar(
                title = "Dark Mode Studio",
                subtitle = "execution",
                onNotificationClick = onNotificationClick,
                onAvatarClick = onAvatarClick
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DmsSpacing.ScreenHorizontal),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Subtitle
                item {
                    Column {
                        Text(
                            text = "Execution",
                            style = DmsTheme.typography.displayL
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your command board.",
                            style = DmsTheme.typography.bodySmall.copy(color = DmsColors.White64)
                        )
                    }
                }

                // Summary Metrics (12 Done, 8 Pending, 2 Blocked, 3 Overdue)
                item {
                    DmsCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DmsRadii.ShapeR16,
                        backgroundColor = DmsColors.Surface01,
                        padding = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricCol("12", "Done", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("8", "Pending", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("2", "Blocked", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("3", "Overdue", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // TODAY'S FOCUS (Vertical Time Rail)
                item {
                    DmsCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DmsRadii.ShapeR18,
                        backgroundColor = DmsColors.Surface01,
                        padding = 14.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Today's Focus",
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )
                                Text(
                                    text = "3 actions scheduled",
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                )
                            }

                            val focusItems = listOf(
                                TimeRailItem(
                                    time = "09:00",
                                    title = "Review PR #342",
                                    subtitle = "Auth token lifecycle edge case",
                                    isCurrent = true,
                                    agent = "Codex"
                                ),
                                TimeRailItem(
                                    time = "11:00",
                                    title = "Push build to Internal Track",
                                    subtitle = "Google Play Console v1.0.0-rc2",
                                    isCurrent = false,
                                    agent = "Claude"
                                ),
                                TimeRailItem(
                                    time = "16:30",
                                    title = "Confirm deployment",
                                    subtitle = "Edge logs & Cloudflare Worker telemetry",
                                    isCurrent = false,
                                    agent = "Antigravity"
                                )
                            )

                            DmsVerticalTimeRail(items = focusItems)
                        }
                    }
                }

                // TASK FEED FILTERS
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("All Tasks", "My Tasks", "Watchlist")
                        items(filters) { filter ->
                            DmsFilterCapsule(
                                text = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }

                // TASK ITEMS
                items(filteredTasks) { task ->
                    TaskFeedCard(
                        task = task,
                        onToggle = {
                            coroutineScope.launch {
                                taskRepository.toggleTask(task.id, task.status)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button: Pure white circle with black plus
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 80.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(DmsColors.White)
                .clickable { onAddTaskClick?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                tint = DmsColors.OledBlack,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MetricCol(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = DmsTheme.typography.h3.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = label,
            style = DmsTheme.typography.caption.copy(
                fontSize = 9.5.sp,
                color = DmsColors.White48
            )
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(DmsColors.White14)
    )
}

@Composable
private fun TaskFeedCard(
    task: Task,
    onToggle: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE

    DmsCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DmsRadii.ShapeR16,
        backgroundColor = DmsColors.Surface01,
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Interactive Checkbox Box
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(DmsRadii.ShapeR8)
                        .background(if (isDone) DmsColors.White else DmsColors.Surface02)
                        .border(
                            BorderStroke(1.dp, if (isDone) DmsColors.White else DmsColors.White32),
                            DmsRadii.ShapeR8
                        )
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Completed",
                            tint = DmsColors.OledBlack,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Task Details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        style = DmsTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isDone) DmsColors.White48 else DmsColors.White
                        )
                    )

                    if (task.description != null) {
                        Text(
                            text = task.description,
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = DmsColors.White48
                            )
                        )
                    }

                    // Capsules row: Project, Priority, Assigned Agent, Due
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        DmsStatusCapsule(
                            text = task.projectName,
                            height = 20.dp,
                            borderColor = DmsColors.White20
                        )

                        if (task.assignedAgent != null) {
                            DmsStatusCapsule(
                                text = task.assignedAgent,
                                height = 20.dp,
                                borderColor = DmsColors.White14
                            )
                        }

                        Text(
                            text = "Due ${task.dueTime}",
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 9.5.sp,
                                color = DmsColors.White48
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = DmsColors.White32,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
