package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

enum class ActionType {
    NEW_TASK,
    NEW_PROJECT,
    NEW_REMINDER,
    NEW_AUTOMATION,
    CONNECT_SERVICE
}

data class ActionOption(
    val type: ActionType,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalActionSheet(
    currentRoute: String,
    onDismissRequest: () -> Unit,
    onSelectAction: (ActionType) -> Unit
) {
    val allActions = listOf(
        ActionOption(ActionType.NEW_TASK, "New Task", "Assign execution work item to project or agent", Icons.Outlined.CheckCircleOutline),
        ActionOption(ActionType.NEW_PROJECT, "New Project", "Initialize new project space and milestones", Icons.Outlined.FolderOpen),
        ActionOption(ActionType.NEW_REMINDER, "New Reminder", "Schedule timed alert for focus or KPI review", Icons.Outlined.Schedule),
        ActionOption(ActionType.CONNECT_SERVICE, "Connect Service", "Add GitHub, Cloudflare, or AI agent credentials", Icons.Outlined.AccountTree),
        ActionOption(ActionType.NEW_AUTOMATION, "New Automation Rule", "Define event-driven notification trigger", Icons.Outlined.Bolt)
    )

    // Context-aware reordering
    val orderedActions = when (currentRoute) {
        "projects" -> listOf(allActions[1]) + allActions.filterIndexed { index, _ -> index != 1 }
        "execution" -> listOf(allActions[0]) + allActions.filterIndexed { index, _ -> index != 0 }
        "updates" -> listOf(allActions[2]) + allActions.filterIndexed { index, _ -> index != 2 }
        else -> allActions
    }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Create",
        subtitle = "Command Center Quick Actions"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            orderedActions.forEach { action ->
                ActionRowItem(
                    action = action,
                    onClick = {
                        onDismissRequest()
                        onSelectAction(action.type)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionRowItem(
    action: ActionOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DmsRadii.ShapeR14)
            .background(DmsColors.Surface02)
            .border(BorderStroke(1.dp, DmsColors.White14), DmsRadii.ShapeR14)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(DmsRadii.ShapeR10)
                .background(DmsColors.SurfaceSelected),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = DmsColors.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.title,
                style = DmsTheme.typography.bodySmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = DmsColors.White
                )
            )
            Text(
                text = action.description,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White48
                )
            )
        }
    }
}
