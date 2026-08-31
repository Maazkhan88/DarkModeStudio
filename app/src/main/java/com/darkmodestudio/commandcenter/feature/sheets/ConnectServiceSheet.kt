package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.darkmodestudio.commandcenter.core.auth.AuthMethod
import com.darkmodestudio.commandcenter.core.auth.ConnectionState
import com.darkmodestudio.commandcenter.core.auth.ProviderCategory
import com.darkmodestudio.commandcenter.core.auth.ProviderDefinition
import com.darkmodestudio.commandcenter.core.auth.ProviderRegistry
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsModalBottomSheet
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.security.SecureProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectServiceSheet(
    initialProviderId: String? = null,
    onDismissRequest: () -> Unit,
    onLaunchOAuth: ((providerId: String) -> Unit)? = null,
    onLaunchDesktopPairing: (() -> Unit)? = null,
    onSaveTokenFallback: ((provider: SecureProvider, token: String, alias: String) -> Unit)? = null
) {
    val allProviders = remember { ProviderRegistry.getProviders() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProviderCategory.ALL) }
    var selectedProvider by remember {
        mutableStateOf(allProviders.find { it.id == initialProviderId } ?: allProviders.first())
    }

    var isAdvancedTokenMode by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var aliasInput by remember { mutableStateOf("${selectedProvider.displayName} Token") }

    val filteredProviders = remember(searchQuery, selectedCategory) {
        val byCat = ProviderRegistry.getProvidersByCategory(selectedCategory)
        if (searchQuery.isBlank()) byCat else ProviderRegistry.searchProviders(searchQuery)
    }

    DmsModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Connect Auth",
        subtitle = "Official sign-in, desktop runtime sessions & secure Keystore storage"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Category Filter Rail
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProviderCategory.values().forEach { cat ->
                    DmsFilterCapsule(
                        text = cat.displayName,
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            // Search Bar
            DmsTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search integrations, AI agents, cloud...",
                label = "Filter Providers",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = DmsColors.White48,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            // Provider Quick Selector
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredProviders.forEach { provider ->
                    DmsFilterCapsule(
                        text = provider.displayName,
                        isSelected = selectedProvider.id == provider.id,
                        onClick = {
                            selectedProvider = provider
                            aliasInput = "${provider.displayName} Token"
                            isAdvancedTokenMode = false
                        }
                    )
                }
            }

            // Active Provider Details Card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR16,
                backgroundColor = DmsColors.Surface01,
                padding = 14.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(DmsRadii.ShapeR8)
                                    .background(DmsColors.SurfaceSelected)
                                    .border(BorderStroke(1.dp, DmsColors.White20), DmsRadii.ShapeR8),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedProvider.iconTag,
                                    style = DmsTheme.typography.label.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DmsColors.White
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = selectedProvider.displayName,
                                    style = DmsTheme.typography.h3.copy(fontSize = 15.sp)
                                )
                                Text(
                                    text = selectedProvider.category.displayName,
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                )
                            }
                        }
                    }

                    Text(
                        text = selectedProvider.description,
                        style = DmsTheme.typography.body.copy(fontSize = 12.sp, color = DmsColors.White80)
                    )

                    // Capabilities Tags
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedProvider.capabilities.forEach { cap ->
                            Box(
                                modifier = Modifier
                                    .clip(DmsRadii.ShapeR8)
                                    .background(DmsColors.Surface02)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = cap.label,
                                    style = DmsTheme.typography.caption.copy(fontSize = 9.5.sp, color = DmsColors.White64)
                                )
                            }
                        }
                    }
                }
            }

            // RECOMMENDED OFFICIAL SIGN-IN / RUNTIME FLOW
            if (!isAdvancedTokenMode) {
                if (selectedProvider.runtimeRequired) {
                    // Agent Runtime Flow via Paired Desktop Host
                    DmsCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DmsRadii.ShapeR12,
                        backgroundColor = DmsColors.Surface02,
                        padding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Computer,
                                    contentDescription = null,
                                    tint = DmsColors.White80,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Desktop Host Bridge Required",
                                    style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                text = "Agents run inside your authenticated desktop CLI session. No service passwords or raw subscription keys are ever transferred to mobile.",
                                style = DmsTheme.typography.caption.copy(fontSize = 10.5.sp, color = DmsColors.White64)
                            )
                        }
                    }

                    DmsPrimaryButton(
                        text = selectedProvider.recommendedActionLabel,
                        onClick = {
                            onLaunchDesktopPairing?.invoke()
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (selectedProvider.authMethods.contains(AuthMethod.OAuthPkce) || selectedProvider.authMethods.contains(AuthMethod.OAuthBackend)) {
                    // Official OAuth Flow
                    DmsPrimaryButton(
                        text = selectedProvider.recommendedActionLabel,
                        onClick = {
                            onLaunchOAuth?.invoke(selectedProvider.id)
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ADVANCED / TOKEN FALLBACK MODE
            if (isAdvancedTokenMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DmsTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = "Token Alias",
                        placeholder = "e.g. Personal Read-Only Token"
                    )

                    DmsTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = "API Key / Personal Access Token",
                        placeholder = "Paste key or token...",
                        singleLine = false,
                        minHeight = 50
                    )

                    DmsPrimaryButton(
                        text = "Save Token in Android Keystore",
                        enabled = tokenInput.isNotBlank(),
                        onClick = {
                            val secProvider = when (selectedProvider.id) {
                                "github" -> SecureProvider.GITHUB
                                "cloudflare" -> SecureProvider.CLOUDFLARE
                                "openai_api" -> SecureProvider.OPENAI
                                "anthropic_api" -> SecureProvider.ANTHROPIC
                                "supabase" -> SecureProvider.SUPABASE
                                else -> SecureProvider.CUSTOM
                            }
                            onSaveTokenFallback?.invoke(secProvider, tokenInput.trim(), aliasInput.trim())
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Advanced Mode Toggle
            if (selectedProvider.authMethods.any { it is AuthMethod.ApiKey || it is AuthMethod.ApiToken || it is AuthMethod.CustomCredential }) {
                DmsSecondaryButton(
                    text = if (isAdvancedTokenMode) "« Back to Recommended Sign-in" else "Advanced: Use API Key / Token",
                    onClick = { isAdvancedTokenMode = !isAdvancedTokenMode },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Security Baseline Notice
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
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = DmsColors.White64,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Dark Mode Studio never collects service passwords. All secrets are stored in on-device Android Keystore with AES-GCM encryption.",
                        style = DmsTheme.typography.caption.copy(fontSize = 9.5.sp, color = DmsColors.White48)
                    )
                }
            }
        }
    }
}
