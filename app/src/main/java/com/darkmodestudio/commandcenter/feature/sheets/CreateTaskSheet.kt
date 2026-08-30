package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.TaskPriority

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskSheet(
    onDismissRequest: () -> Unit,
    onSubmit: (title: String, description: String?, projectId: String, projectName: String, priority: TaskPriority, agent: String?, dueTime: String) -> Unit,
    preselectedProjectId: String? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val projects = remember {
        listOf(
            "secondme" to "SecondMe",
            "ghostcart" to "GhostCart",
            "proptree" to "Proptree",
            "agstudio" to "AG Studio",
            "pioneer" to "Pioneer"
        )
    }
    var selectedProject by remember {
        mutableStateOf(
            projects.find { it.first == preselectedProjectId } ?: projects.first()
        )
    }

    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH) }
    var selectedAgent by remember { mutableStateOf<String?>("Codex") }
    val agents = listOf("Codex", "Claude", "Antigravity", "None")

    var dueTime by remember { mutableStateOf("Today") }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "New Task",
        subtitle = "Assign execution item to project or agent"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Task Title
            DmsTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "e.g. Implement vector memory index",
                label = "Task Title"
            )

            // Task Description
            DmsTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = "Optional details or sub-actions",
                label = "Description",
                singleLine = false,
                minHeight = 60
            )

            // Project Selector
            Column {
                Text(
                    text = "Project",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    projects.forEach { (id, name) ->
                        DmsFilterCapsule(
                            text = name,
                            isSelected = selectedProject.first == id,
                            onClick = { selectedProject = id to name }
                        )
                    }
                }
            }

            // Priority Selector
            Column {
                Text(
                    text = "Priority",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TaskPriority.entries.forEach { priority ->
                        DmsFilterCapsule(
                            text = priority.displayName,
                            isSelected = selectedPriority == priority,
                            onClick = { selectedPriority = priority }
                        )
                    }
                }
            }

            // Assigned Agent
            Column {
                Text(
                    text = "Assigned Agent",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    agents.forEach { agent ->
                        val isSelected = if (agent == "None") selectedAgent == null else selectedAgent == agent
                        DmsFilterCapsule(
                            text = agent,
                            isSelected = isSelected,
                            onClick = {
                                selectedAgent = if (agent == "None") null else agent
                            }
                        )
                    }
                }
            }

            // Due Time
            DmsTextField(
                value = dueTime,
                onValueChange = { dueTime = it },
                placeholder = "e.g. 11:00 or Today",
                label = "Due Time"
            )

            Spacer(modifier = Modifier.height(6.dp))

            DmsPrimaryButton(
                text = "Create Task",
                enabled = title.isNotBlank(),
                onClick = {
                    onSubmit(
                        title.trim(),
                        description.trim().ifEmpty { null },
                        selectedProject.first,
                        selectedProject.second,
                        selectedPriority,
                        selectedAgent,
                        dueTime.trim().ifEmpty { "Today" }
                    )
                    onDismissRequest()
                }
            )
        }
    }
}
