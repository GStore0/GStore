/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.details.composable

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.gplayapi.data.models.App
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.composable.app.TagListItem
import com.kangrio.gstore.compose.preview.AppPreviewProvider
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.util.CommonUtil

/**
 * Composable to display tags related to the app, supposed to be used as a part
 * of the Column with proper vertical arrangement spacing in the AppDetailsScreen.
 * @param app App to show tags
 */
@Composable
fun Tags(app: App) {
    val context = LocalContext.current

    val installsLabel = CommonUtil.addDiPrefix(app.installs)
    val averageRating = if (app.labeledRating == "0.0" || app.labeledRating.isBlank()) {
        null
    } else {
        app.labeledRating
    }
    val paidLabel = if (app.isFree) {
        stringResource(R.string.details_free)
    } else {
        stringResource(R.string.details_paid)
    }
    val adsLabel = if (app.containsAds) {
        stringResource(R.string.details_contains_ads)
    } else {
        stringResource(R.string.details_no_ads)
    }

    val tags = mapOf(
        averageRating to R.drawable.ic_star,
        installsLabel to R.drawable.ic_download_manager,
        Formatter.formatShortFileSize(context, app.size) to R.drawable.ic_apk_install,
        app.updatedOn to R.drawable.ic_updates,
        paidLabel to R.drawable.ic_paid,
        adsLabel to R.drawable.ic_campaign
    ).filterKeys { it != null }

    LazyRow(
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        items(items = tags.keys.toList()) { label ->
            TagListItem(label = label!!, painter = painterResource(tags.getValue(label)))
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun TagsPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Tags(app = app)
    }
}
