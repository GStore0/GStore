/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.model

import com.aurora.gplayapi.data.models.App

data class PaginatedAppList(
    val appList: MutableList<App> = mutableListOf(),
    var hasMore: Boolean
)
