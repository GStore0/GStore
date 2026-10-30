/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.composable

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.data.model.Permission
import com.kangrio.gstore.data.model.PermissionType

@Composable
fun PermissionListItem(
    modifier: Modifier = Modifier,
    permission: Permission,
    onAction: () -> Unit = {}
) {
    GStoreListItem(
        modifier = modifier,
        headline = permission.title,
        supporting = permission.subtitle,
        trailing = {
            TextButton(onClick = onAction, enabled = !permission.isGranted) {
                Text(
                    text = if (permission.isGranted) {
                        stringResource(R.string.action_granted)
                    } else {
                        stringResource(R.string.action_grant)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun PermissionListItemPreview() {
    PermissionListItem(
        permission = Permission(
            PermissionType.STORAGE_MANAGER,
            stringResource(R.string.onboarding_permission_esm),
            stringResource(R.string.onboarding_permission_esa_desc),
            false
        )
    )
}
