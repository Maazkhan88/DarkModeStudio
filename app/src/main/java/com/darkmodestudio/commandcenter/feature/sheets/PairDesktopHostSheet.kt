package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.agent.DesktopHostBridge
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairDesktopHostSheet(
    desktopHostBridge: DesktopHostBridge,
    onDismiss: () -> Unit,
    onPairSuccess: (hostName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var hostAddressInput by remember { mutableStateOf("192.168.0.137:8998") }
    var hostNameInput by remember { mutableStateOf("MK-Lenovo") }
    var pairingCodeInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DmsColors.OledBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Computer,
                        contentDescription = null,
                        tint = DmsColors.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Pair Desktop Host",
                        style = DmsTheme.typography.h3
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

            Text(
                text = "Connect mobile Dark Mode Studio to your developer workstation running local agent CLIs (Codex, Claude Code, Antigravity).",
                style = DmsTheme.typography.body.copy(fontSize = 12.sp, color = DmsColors.White64)
            )

            // Setup Instructions Card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR12,
                backgroundColor = DmsColors.Surface02,
                padding = 10.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "How to Pair:",
                        style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    )
                    Text(
                        text = "1. Run `npm start -- --port 8998` in /desktop on your computer.\n2. Tap 'Generate Pairing Code' on desktop UI or console.\n3. Enter computer LAN IP and 6-digit code below.",
                        style = DmsTheme.typography.caption.copy(fontSize = 10.sp, color = DmsColors.White64, lineHeight = 14.sp)
                    )
                }
            }

            // Error Notice
            if (!errorMessage.isNullOrBlank()) {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR12,
                    backgroundColor = DmsColors.Surface02,
                    padding = 10.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = DmsColors.White80,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage!!,
                            style = DmsTheme.typography.caption.copy(fontSize = 10.5.sp, color = DmsColors.White92)
                        )
                    }
                }
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DmsTextField(
                        value = hostAddressInput,
                        onValueChange = {
                            hostAddressInput = it
                            errorMessage = null
                        },
                        label = "Desktop Host Address (IP:Port)",
                        placeholder = "LAN: 192.168.0.137:8998 or Tailscale: 100.x.x.x:8998"
                    )

                    val cleanAddr = hostAddressInput.trim()
                    if (cleanAddr.isNotBlank()) {
                        val connectionType = when {
                            cleanAddr.startsWith("100.") -> "Tailscale / Private Remote"
                            cleanAddr.startsWith("192.168.") || cleanAddr.startsWith("10.") -> "Local Network (Wi-Fi / LAN)"
                            cleanAddr.startsWith("127.") || cleanAddr.startsWith("localhost") -> "Localhost (Device-only Loopback)"
                            else -> "Network Host"
                        }
                        Text(
                            text = "Connection Type: $connectionType",
                            style = DmsTheme.typography.caption.copy(fontSize = 10.sp, color = DmsColors.White64)
                        )
                    }
                }

                DmsTextField(
                    value = hostNameInput,
                    onValueChange = {
                        hostNameInput = it
                        errorMessage = null
                    },
                    label = "Computer Name",
                    placeholder = "e.g. MK-Lenovo / Workstation"
                )

                DmsTextField(
                    value = pairingCodeInput,
                    onValueChange = {
                        pairingCodeInput = it
                        errorMessage = null
                    },
                    label = "Pairing Code (from Desktop)",
                    placeholder = "e.g. DMS-570176"
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DmsSecondaryButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                DmsPrimaryButton(
                    text = if (isSubmitting) "Pairing..." else "Pair Computer",
                    enabled = !isSubmitting && hostAddressInput.isNotBlank() && pairingCodeInput.isNotBlank(),
                    onClick = {
                        isSubmitting = true
                        errorMessage = null
                        scope.launch {
                            val result = desktopHostBridge.pairHost(
                                hostAddress = hostAddressInput.trim(),
                                hostName = hostNameInput.trim(),
                                pairingCode = pairingCodeInput.trim()
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                onPairSuccess(result.hostName ?: hostNameInput)
                                onDismiss()
                            } else {
                                errorMessage = result.errorMessage ?: "Pairing failed. Please check host address and code."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = DmsColors.White48,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Pairing tokens are cryptographically generated and encrypted using Android Keystore-backed AES-256-GCM keys. Zero secrets stored in SQLite.",
                    style = DmsTheme.typography.caption.copy(fontSize = 9.sp, color = DmsColors.White48)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
