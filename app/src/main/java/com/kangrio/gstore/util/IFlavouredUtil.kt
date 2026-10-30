/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.util

import android.content.Context

interface IFlavouredUtil {
    val defaultDispensers: Set<String>
    fun promptMicroGInstall(context: Context): Boolean
}
