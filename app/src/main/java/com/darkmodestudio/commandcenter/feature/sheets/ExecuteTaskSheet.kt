package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.agent.DesktopHostBridge
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Task
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteTaskSheet(
    task: Task,
    desktopHostBridge: DesktopHostBridge,
    onDismiss: () -> Unit,
    onTaskCompleted: (Task) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val agentName = task.assignedAgent ?: "Antigravity"
    var promptInput by remember {
        mutableStateOf(
            buildString {
                append(task.title)
                if (!task.description.isNullOrBlank()) {
                    append("\n\nDetails: ")
                    append(task.description)
                }
            }
        )
    }

    var isRunning by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf<String?>(null) }
    var executionLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var executionSuccess by remember { mutableStateOf<Boolean?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DmsColors.OledBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Execute Task",
                        style = DmsTheme.typography.h3
                    )
                    Text(
                        text = "Dispatched via paired desktop engine",
                        style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = DmsColors.White64
                    )
                }
            }

            // Task Meta Card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR16,
                backgroundColor = DmsColors.Surface01,
                padding = 14.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = task.title,
                        style = DmsTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DmsStatusCapsule(
                            text = task.projectName,
                            height = 20.dp,
                            borderColor = DmsColors.White20
                        )

                        DmsStatusCapsule(
                            text = agentName,
                            height = 20.dp,
                            borderColor = DmsColors.White
                        )

                        DmsStatusCapsule(
                            text = "MK-Lenovo",
                            height = 20.dp,
                            borderColor = DmsColors.White14
                        )
                    }
                }
            }

            // Prompt Input
            DmsTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                label = "Instructions for $agentName",
                placeholder = "Prompt to execute on desktop CLI",
                singleLine = false,
                minHeight = 70
            )

            // Live Terminal Console Output
            if (isRunning || executionResult != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = DmsColors.White80,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isRunning) "Executing on MK-Lenovo..." else "Terminal Output",
                            style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White80)
                        )
                        if (isRunning) {
                            Spacer(modifier = Modifier.weight(1f))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = DmsColors.White,
                                strokeWidth = 1.5.dp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DmsRadii.ShapeR12)
                            .background(DmsColors.Surface02)
                            .border(1.dp, DmsColors.White14, DmsRadii.ShapeR12)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isRunning) {
                                Text(
                                    text = ">> Starting session with $agentName...\n>> Connecting to http://100.67.224.107:8998\n>> Sending prompt to desktop CLI...",
                                    style = DmsTheme.typography.caption.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = DmsColors.White64
                                    )
                                )
                            } else {
                                executionLogs.forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        style = DmsTheme.typography.caption.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            color = DmsColors.White80
                                        )
                                    )
                                }
                                Text(
                                    text = executionResult ?: "",
                                    style = DmsTheme.typography.caption.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (executionSuccess == true) DmsColors.White else DmsColors.White48
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (executionSuccess == true) {
                    DmsPrimaryButton(
                        text = "Mark Done & Close",
                        onClick = {
                            onTaskCompleted(task)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    DmsSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    DmsPrimaryButton(
                        text = if (isRunning) "Running..." else "Run on Desktop",
                        enabled = !isRunning && promptInput.isNotBlank(),
                        onClick = {
                            isRunning = true
                            executionResult = null
                            executionLogs = emptyList()
                            executionSuccess = null

                            scope.launch {
                                val session = desktopHostBridge.startSession(
                                    agentId = agentName.lowercase(),
                                    project = task.projectId
                                )
                                val result = desktopHostBridge.sendPrompt(
                                    agentId = agentName.lowercase(),
                                    sessionId = session.sessionId,
                                    prompt = promptInput.trim()
                                )
                                isRunning = false
                                executionSuccess = result.isSuccess
                                executionResult = result.summary
                                executionLogs = result.logs
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
