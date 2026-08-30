package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderSheet(
    onDismissRequest: () -> Unit,
    onSubmit: (title: String, dueText: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("09:30 AM") }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "New Reminder",
        subtitle = "Schedule timed command-center focus notification"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            DmsTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "e.g. Weekly KPI Review",
                label = "Reminder Title"
            )

            DmsTextField(
                value = dueText,
                onValueChange = { dueText = it },
                placeholder = "e.g. 02:00 PM or In 30 mins",
                label = "Scheduled Time"
            )

            Spacer(modifier = Modifier.height(8.dp))

            DmsPrimaryButton(
                text = "Schedule Reminder",
                enabled = title.isNotBlank(),
                onClick = {
                    onSubmit(title.trim(), dueText.trim().ifEmpty { "09:00 AM" })
                    onDismissRequest()
                }
            )
        }
    }
}
