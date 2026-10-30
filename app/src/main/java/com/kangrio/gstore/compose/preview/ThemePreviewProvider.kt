/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import coil3.compose.LocalAsyncImagePreviewHandler
import com.kangrio.gstore.compose.theme.GStoreTheme

/**
 * Preview provider for default theme and remote image handling
 */
class ThemePreviewProvider : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        GStoreTheme {
            CompositionLocalProvider(
                value = LocalAsyncImagePreviewHandler provides coilPreviewProvider,
                content = content
            )
        }
    }
}
