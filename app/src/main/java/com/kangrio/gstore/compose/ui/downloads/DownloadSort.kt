/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.downloads

import android.content.Context
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.ui.commons.SortOrder
import com.kangrio.gstore.compose.ui.commons.enumValueOrDefault
import com.kangrio.gstore.data.model.DownloadSortBy
import com.kangrio.gstore.util.Preferences

data class DownloadSort(
    val sortBy: DownloadSortBy = DownloadSortBy.DATE_DOWNLOADED,
    val sortOrder: SortOrder = SortOrder.DESC
)

fun DownloadSort.save(context: Context) {
    Preferences.putString(context, Preferences.PREFERENCE_DOWNLOADS_SORT_BY, sortBy.name)
    Preferences.putString(context, Preferences.PREFERENCE_DOWNLOADS_SORT_ORDER, sortOrder.name)
}

fun loadDownloadSort(context: Context): DownloadSort {
    val default = DownloadSort()
    return DownloadSort(
        sortBy = enumValueOrDefault(
            Preferences.getString(context, Preferences.PREFERENCE_DOWNLOADS_SORT_BY),
            default.sortBy
        ),
        sortOrder = enumValueOrDefault(
            Preferences.getString(context, Preferences.PREFERENCE_DOWNLOADS_SORT_ORDER),
            default.sortOrder
        )
    )
}

fun DownloadSortBy.labelRes(): Int = when (this) {
    DownloadSortBy.DATE_DOWNLOADED -> R.string.download_sort_date_downloaded
    DownloadSortBy.NAME -> R.string.installed_sort_name
    DownloadSortBy.SIZE -> R.string.installed_sort_size
}
