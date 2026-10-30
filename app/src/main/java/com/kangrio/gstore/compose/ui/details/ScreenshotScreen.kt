/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kangrio.gstore.extensions.adaptiveNavigationIcon
import com.kangrio.gstore.extensions.isWindowCompact
import com.aurora.gplayapi.data.models.Artwork
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.composable.TopAppBar
import com.kangrio.gstore.compose.composable.details.ScreenshotListItem
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.viewmodel.details.AppDetailsViewModel

@Composable
fun ScreenshotScreen(
    packageName: String,
    index: Int,
    viewModel: AppDetailsViewModel = hiltViewModel(key = packageName),
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val screenshots = app?.screenshots ?: emptyList()

    val topAppBarTitle = when {
        windowAdaptiveInfo.isWindowCompact -> app!!.displayName
        else -> stringResource(R.string.details_more_about_app)
    }

    ScreenContent(
        topAppBarTitle = topAppBarTitle,
        screenshots = screenshots.distinctBy { it.url },
        index = index
    )
}

@Composable
private fun ScreenContent(
    topAppBarTitle: String? = null,
    screenshots: List<Artwork> = emptyList(),
    index: Int = 0,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2()
) {
    val displayMetrics = LocalResources.current.displayMetrics
    val pagerState = rememberPagerState(initialPage = index) { screenshots.size }

    LaunchedEffect(key1 = index) {
        if (pagerState.currentPage != index) pagerState.scrollToPage(index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = topAppBarTitle,
                navigationIcon = windowAdaptiveInfo.adaptiveNavigationIcon
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = pagerState,
            key = { screenshots[it].url }
        ) { page ->
            val artwork = screenshots[page]
            ScreenshotListItem(
                modifier = Modifier.fillMaxSize(),
                url = "${artwork.url}=rw-w${displayMetrics.widthPixels}-v1-e15"
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun ScreenshotScreenPreview() {
    ScreenContent(
        topAppBarTitle = stringResource(R.string.app_name)
    )
}
