/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.spoof.navigation

import androidx.annotation.StringRes
import com.kangrio.gstore.R

/**
 * Pages that are shown in SpoofScreen
 */
enum class SpoofPage(@StringRes val localized: Int) {
    DEVICE(R.string.title_device),
    LOCALE(R.string.title_language)
}
