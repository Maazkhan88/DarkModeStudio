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
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.security.SecureProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectServiceSheet(
    onDismissRequest: () -> Unit,
    onSubmit: (provider: SecureProvider, token: String, alias: String) -> Unit
) {
    val providers = remember {
        listOf(
            SecureProvider.GITHUB to "GitHub",
            SecureProvider.CLOUDFLARE to "Cloudflare",
            SecureProvider.OPENAI to "OpenAI",
            SecureProvider.ANTHROPIC to "Anthropic",
            SecureProvider.SUPABASE to "Supabase"
        )
    }
    var selectedProvider by remember { mutableStateOf(providers.first()) }
    var token by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("Personal Read-Only Key") }

    val placeholderText = when (selectedProvider.first) {
        SecureProvider.GITHUB -> "ghp_..."
        SecureProvider.CLOUDFLARE -> "Cloudflare API Token"
        SecureProvider.OPENAI -> "sk-proj-..."
        SecureProvider.ANTHROPIC -> "sk-ant-..."
        SecureProvider.SUPABASE -> "eyJhbGciOi..."
        else -> "API Secret Key"
    }

    val hintText = when (selectedProvider.first) {
        SecureProvider.GITHUB -> "Create a Personal Access Token with 'repo', 'read:org', and 'workflow' scopes at github.com/settings/tokens."
        SecureProvider.CLOUDFLARE -> "Create an API Token with Zone:Read and Workers:Read permissions at dash.cloudflare.com/profile/api-tokens."
        SecureProvider.OPENAI -> "Create a secret API key at platform.openai.com/api-keys."
        SecureProvider.ANTHROPIC -> "Generate an API key in the Anthropic Console at console.anthropic.com."
        SecureProvider.SUPABASE -> "Copy your Project URL and anon / service_role key from Supabase Project Settings -> API."
        else -> "Credentials are stored securely inside Android Keystore."
    }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Connect Service",
        subtitle = "Store encrypted credentials using Android Keystore"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Provider Selector
            Column {
                Text(
                    text = "Select Provider",
                    style = DmsTheme.typography.caption.copy(fontSize = 11.sp, color = DmsColors.White64),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    providers.forEach { (p, name) ->
                        DmsFilterCapsule(
                            text = name,
                            isSelected = selectedProvider.first == p,
                            onClick = {
                                selectedProvider = p to name
                                alias = "$name API Key"
                            }
                        )
                    }
                }
            }

            // Key Alias
            DmsTextField(
                value = alias,
                onValueChange = { alias = it },
                placeholder = "e.g. Work GitHub PAT",
                label = "Key Label / Alias"
            )

            // API Token / Key
            DmsTextField(
                value = token,
                onValueChange = { token = it },
                placeholder = placeholderText,
                label = "API Token / Secret",
                singleLine = false,
                minHeight = 50
            )

            // Provider-specific instructions card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR12,
                backgroundColor = DmsColors.Surface02,
                padding = 10.dp
            ) {
                Text(
                    text = hintText,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.5.sp,
                        color = DmsColors.White80,
                        lineHeight = 15.sp
                    )
                )
            }

            // Keystore info card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR12,
                backgroundColor = DmsColors.Surface02,
                padding = 10.dp
            ) {
                Text(
                    text = "Android Keystore Encryption: Keys are encrypted on-device with AES-256-GCM master keys and never transmitted to third parties.",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        color = DmsColors.White48
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            DmsPrimaryButton(
                text = "Save & Secure Credential",
                enabled = token.isNotBlank(),
                onClick = {
                    onSubmit(selectedProvider.first, token.trim(), alias.trim())
                    onDismissRequest()
                }
            )
        }
    }
}
