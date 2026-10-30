/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.composable

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.data.installer.SessionInstaller
import com.kangrio.gstore.data.model.InstallerInfo

@Composable
fun InstallerListItem(
    modifier: Modifier = Modifier,
    installerInfo: InstallerInfo,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val description = stringResource(installerInfo.description)

    GStoreListItem(
        modifier = modifier,
        headline = stringResource(installerInfo.title),
        supporting = stringResource(installerInfo.subtitle),
        // Name the app providing this installer, where several can serve the same one
        tertiary = installerInfo.provider
            ?.let { stringResource(R.string.installer_provider, description, it) }
            ?: description,
        onClick = onClick,
        trailing = { RadioButton(selected = isSelected, onClick = onClick) }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InstallerListItemPreview() {
    InstallerListItem(installerInfo = SessionInstaller.installerInfo, isSelected = true)
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InstallerListItemProviderPreview() {
    InstallerListItem(
        installerInfo = SessionInstaller.installerInfo.copy(provider = "Shevery"),
        isSelected = true
    )
}
