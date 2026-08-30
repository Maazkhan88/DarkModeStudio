package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsToggle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.ProjectStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProjectSheet(
    onDismissRequest: () -> Unit,
    onSubmit: (name: String, description: String, iconTag: String, status: ProjectStatus, dueDate: String, nextMilestone: String, isMvp: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var iconTag by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("Oct 15, 2026") }
    var nextMilestone by remember { mutableStateOf("Core Architecture") }
    var status by remember { mutableStateOf(ProjectStatus.ON_TRACK) }
    var isMvp by remember { mutableStateOf(false) }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "New Project",
        subtitle = "Initialize command center project & milestone rail"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Project Name
            DmsTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (iconTag.isEmpty() && it.isNotEmpty()) {
                        iconTag = it.take(2).uppercase()
                    }
                },
                placeholder = "e.g. Tonecast Engine",
                label = "Project Name"
            )

            // Monogram & Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DmsTextField(
                    value = iconTag,
                    onValueChange = { if (it.length <= 3) iconTag = it.uppercase() },
                    placeholder = "TE",
                    label = "Icon Tag (2-3 chars)",
                    modifier = Modifier.weight(1f)
                )

                DmsTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    placeholder = "Nov 01, 2026",
                    label = "Target Due Date",
                    modifier = Modifier.weight(1.5f)
                )
            }

            // Description
            DmsTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Low-latency neural audio processor plugin",
                label = "Description",
                singleLine = false,
                minHeight = 60
            )

            // Next Milestone
            DmsTextField(
                value = nextMilestone,
                onValueChange = { nextMilestone = it },
                placeholder = "e.g. Real-time C++ Core DSP",
                label = "Initial Milestone"
            )

            // Status Selector
            Column {
                Text(
                    text = "Initial Status",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        ProjectStatus.ON_TRACK,
                        ProjectStatus.IN_PROGRESS,
                        ProjectStatus.WAITING,
                        ProjectStatus.BLOCKED
                    ).forEach { s ->
                        DmsFilterCapsule(
                            text = s.displayName,
                            isSelected = status == s,
                            onClick = { status = s }
                        )
                    }
                }
            }

            // MVP Flag Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Flag as MVP Phase",
                        style = DmsTheme.typography.bodySmall.copy(
                            color = DmsColors.White,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = "Displays MVP capsule indicator in project hero",
                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                    )
                }
                DmsToggle(
                    checked = isMvp,
                    onCheckedChange = { isMvp = it }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            DmsPrimaryButton(
                text = "Create Project",
                enabled = name.isNotBlank(),
                onClick = {
                    onSubmit(
                        name.trim(),
                        description.trim(),
                        iconTag.trim().ifEmpty { name.take(2).uppercase() },
                        status,
                        dueDate.trim().ifEmpty { "TBD" },
                        nextMilestone.trim().ifEmpty { "Initial Launch" },
                        isMvp
                    )
                    onDismissRequest()
                }
            )
        }
    }
}
