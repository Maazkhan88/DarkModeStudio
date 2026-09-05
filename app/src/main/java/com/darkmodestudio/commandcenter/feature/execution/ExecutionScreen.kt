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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun ExecutionScreen(
    taskRepository: TaskRepository,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onAddTaskClick: (() -> Unit)? = null,
    onTaskClick: ((Task) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val tasks by taskRepository.tasks.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All Tasks") }

    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val pendingCount = tasks.count { it.status == TaskStatus.PENDING }
    val blockedCount = tasks.count { it.status == TaskStatus.BLOCKED }
    val overdueCount = tasks.count { it.status == TaskStatus.OVERDUE }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "My Tasks" -> tasks.filter { it.assignedAgent == "Codex" || it.assignedAgent == "Antigravity" }
            "Watchlist" -> tasks.filter { it.status == TaskStatus.BLOCKED || it.status == TaskStatus.OVERDUE }
            "Done" -> tasks.filter { it.status == TaskStatus.DONE }
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

                // Summary Metrics (Dynamic counts from Room SQLite)
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
                            MetricCol("$doneCount", "Done", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("$pendingCount", "Pending", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("$blockedCount", "Blocked", modifier = Modifier.weight(1f))
                            Divider()
                            MetricCol("$overdueCount", "Overdue", modifier = Modifier.weight(1f))
                        }
                    }
                }

                // TODAY'S FOCUS (Vertical Time Rail with Exact Timestamps)
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
                                    text = "Aug 30, 2026",
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                )
                            }

                            val focusItems = listOf(
                                TimeRailItem(
                                    time = "06:00 PM",
                                    title = "Deploy v1.5.0 live GitHub API",
                                    subtitle = "Verify live commit streaming on DarkModeStudio",
                                    isCurrent = true,
                                    agent = "Antigravity"
                                ),
                                TimeRailItem(
                                    time = "07:30 PM",
                                    title = "Verify Google OAuth Web Client ID",
                                    subtitle = "Credential Manager sign-in on SecondMe",
                                    isCurrent = false,
                                    agent = "Codex"
                                ),
                                TimeRailItem(
                                    time = "10:00 PM",
                                    title = "Confirm deployment & verify telemetry",
                                    subtitle = "Edge logs & Cloudflare Worker metrics",
                                    isCurrent = false,
                                    agent = "Claude"
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
                        val filters = listOf("All Tasks", "My Tasks", "Watchlist", "Done")
                        items(filters) { filter ->
                            DmsFilterCapsule(
                                text = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }

                // TASK ITEMS with Exact Date and Time
                items(filteredTasks) { task ->
                    TaskFeedCard(
                        task = task,
                        onToggle = {
                            coroutineScope.launch {
                                taskRepository.toggleTask(task.id, task.status)
                            }
                        },
                        onClick = {
                            onTaskClick?.invoke(task)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Floating Action Button: Add Task
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
                contentDescription = "New Task",
                tint = DmsColors.OledBlack,
                modifier = Modifier.size(22.dp)
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
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE

    DmsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                // Interactive Checkbox
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

                    // Capsules row: Project, Priority, Assigned Agent, Due Date and Time
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
                            text = if (isDone) "Completed: ${task.dueTime}" else "Due: ${task.dueTime}",
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 9.5.sp,
                                color = DmsColors.White48
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Execute with Agent",
                tint = DmsColors.White80,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onClick)
            )
        }
    }
}
