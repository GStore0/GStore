/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.composable.TopAppBar
import com.kangrio.gstore.compose.navigation.Destination
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.data.model.PermissionType
import com.kangrio.gstore.extensions.toast
import com.kangrio.gstore.patch.ConstantsPatch
import com.kangrio.gstore.util.Preferences

@Composable
fun SettingsScreen(onNavigateTo: (Destination) -> Unit) {
    ScreenContent(onNavigateTo = onNavigateTo)
}

@Composable
private fun ScreenContent(onNavigateTo: (Destination) -> Unit = {}) {
    val context = LocalContext.current
    var useMorphePatches by remember { mutableStateOf(Preferences.getBoolean(context, ConstantsPatch.USE_MORPHE_PATCH_IF_AVAILABLE, true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_settings)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onNavigateTo(
                            Destination.PermissionRationale(PermissionType.entries.toSet())
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_list_check),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(stringResource(R.string.onboarding_title_permissions))
                    }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onNavigateTo(Destination.InstallationPreference)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_installation),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_installation)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.UIPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.pref_ui_title)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onNavigateTo(Destination.NotificationPreference)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification_settings),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_notifications)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.NetworkPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_network),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.pref_network_title)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.UpdatesPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_updates),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_updates)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { onNavigateTo(Destination.SecurityPreference) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_security)) }
                )
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { context.toast("Implement official moprhe patches if available eg: Youtube") },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_experiment),
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.title_morphe)) },
                    trailingContent = {
                        Switch(
                            checked = useMorphePatches,
                            onCheckedChange = {
                                Preferences.putBoolean(context, ConstantsPatch.USE_MORPHE_PATCH_IF_AVAILABLE, it)
                                useMorphePatches = it
                            }
                        )
                    }
                )
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun SettingsScreenPreview() {
    ScreenContent()
}
