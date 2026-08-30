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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAutomationSheet(
    onDismissRequest: () -> Unit,
    onSubmit: (name: String, triggerType: String, actionType: String, humanReadableText: String) -> Unit
) {
    val triggers = remember {
        listOf(
            "GITHUB_WORKFLOW_FAILED" to "GitHub CI Action Fails",
            "PROJECT_INACTIVE_24H" to "Project Has No Activity (24h)",
            "AGENT_QUOTA_HIGH" to "Agent Quota Exceeds 80%",
            "DEPLOYMENT_SUCCESS" to "Edge Deployment Succeeds"
        )
    }

    val actions = remember {
        listOf(
            "SEND_NOTIFICATION" to "Send High-Priority Push Notification",
            "MARK_TASK_COMPLETE" to "Mark Linked Task Complete",
            "TRIGGER_AGENT_RUN" to "Trigger Automated Agent Review"
        )
    }

    var selectedTrigger by remember { mutableStateOf(triggers.first()) }
    var selectedAction by remember { mutableStateOf(actions.first()) }
    var ruleName by remember { mutableStateOf("CI Failure Alert Rule") }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "New Automation",
        subtitle = "Define structured event trigger and command response"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DmsTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                placeholder = "e.g. Build Failure Alert",
                label = "Rule Name"
            )

            // Trigger Selector
            Column {
                Text(
                    text = "Event Trigger (WHEN)",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    triggers.forEach { (type, label) ->
                        DmsFilterCapsule(
                            text = label,
                            isSelected = selectedTrigger.first == type,
                            onClick = { selectedTrigger = type to label }
                        )
                    }
                }
            }

            // Action Selector
            Column {
                Text(
                    text = "Command Action (THEN)",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    actions.forEach { (type, label) ->
                        DmsFilterCapsule(
                            text = label,
                            isSelected = selectedAction.first == type,
                            onClick = { selectedAction = type to label }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            DmsPrimaryButton(
                text = "Save Automation Rule",
                enabled = ruleName.isNotBlank(),
                onClick = {
                    val humanText = "WHEN ${selectedTrigger.second} THEN ${selectedAction.second}"
                    onSubmit(ruleName.trim(), selectedTrigger.first, selectedAction.first, humanText)
                    onDismissRequest()
                }
            )
        }
    }
}
