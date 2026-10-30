/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.compose.ui.favourite

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.kangrio.gstore.Constants.JSON_MIME_TYPE
import com.kangrio.gstore.extensions.emptyPagingItems
import com.kangrio.gstore.extensions.toast
import com.kangrio.gstore.R
import com.kangrio.gstore.compose.composable.ContainedLoadingIndicator
import com.kangrio.gstore.compose.composable.FavouriteListItem
import com.kangrio.gstore.compose.composable.InsufficientStorageDialog
import com.kangrio.gstore.compose.composable.Placeholder
import com.kangrio.gstore.compose.composable.ScrollHint
import com.kangrio.gstore.compose.composable.SectionHeader
import com.kangrio.gstore.compose.composable.TopAppBar
import com.kangrio.gstore.compose.navigation.Destination
import com.kangrio.gstore.compose.preview.FavouritePreviewProvider
import com.kangrio.gstore.compose.preview.ThemePreviewProvider
import com.kangrio.gstore.compose.ui.commons.InstallFavouritesDialog
import com.kangrio.gstore.compose.ui.favourite.menu.FavouriteMenu
import com.kangrio.gstore.compose.ui.favourite.menu.MenuItem
import com.kangrio.gstore.data.model.StorageRequirement
import com.kangrio.gstore.data.room.download.Download
import com.kangrio.gstore.data.room.favourite.Favourite
import com.kangrio.gstore.util.StorageUtil
import com.kangrio.gstore.viewmodel.all.FavouriteViewModel
import java.util.Calendar
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun FavouriteScreen(
    onNavigateTo: (Destination) -> Unit,
    viewModel: FavouriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val favourites = viewModel.favourites.collectAsLazyPagingItems()
    val isEnqueuing by viewModel.isEnqueuing.collectAsStateWithLifecycle()
    val hasInstallableFavourites by viewModel.hasInstallableFavourites
        .collectAsStateWithLifecycle()
    val downloads by viewModel.downloadsList.collectAsStateWithLifecycle()
    val downloadMap = remember(downloads) { downloads.associateBy { it.packageName } }
    var storageWarning by remember { mutableStateOf<StorageRequirement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.storageWarning.collect { storageWarning = it }
    }

    LaunchedEffect(Unit) {
        viewModel.enqueueResult.collect { count ->
            when {
                count > 0 -> context.toast(
                    context.getString(R.string.toast_fav_install_enqueued, count)
                )

                count == 0 -> context.toast(R.string.toast_fav_install_none)
                else -> context.toast(R.string.toast_fav_install_failed)
            }
        }
    }

    val documentOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            if (it != null) {
                viewModel.importFavourites(it)
                context.toast(R.string.toast_fav_import_success)
            } else {
                context.toast(R.string.toast_fav_import_failed)
            }
        }
    )

    val documentCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
        onResult = {
            if (it != null) {
                viewModel.exportFavourites(it)
                context.toast(R.string.toast_fav_export_success)
            } else {
                context.toast(R.string.toast_fav_export_failed)
            }
        }
    )

    ScreenContent(
        favourites = favourites,
        downloads = downloadMap,
        isEnqueuing = isEnqueuing,
        showInstallAll = hasInstallableFavourites,
        onNavigateTo = onNavigateTo,
        onInstallAll = { viewModel.installAll() },
        onRemoveFavourite = { packageName -> viewModel.removeFavourite(packageName) },
        onImportFavourites = {
            documentOpenLauncher.launch(arrayOf(JSON_MIME_TYPE))
        },
        onExportFavourites = {
            documentCreateLauncher.launch(
                "gstore_favourites_${Calendar.getInstance().time.time}.json"
            )
        }
    )

    storageWarning?.let { requirement ->
        InsufficientStorageDialog(
            requirement = requirement,
            onFreeUpSpace = {
                storageWarning = null
                StorageUtil.openFreeUpSpace(context)
            },
            onDismiss = { storageWarning = null }
        )
    }
}

@Composable
private fun ScreenContent(
    favourites: LazyPagingItems<Favourite> = emptyPagingItems(),
    downloads: Map<String, Download> = emptyMap(),
    isEnqueuing: Boolean = false,
    showInstallAll: Boolean = true,
    onNavigateTo: (Destination) -> Unit = {},
    onInstallAll: () -> Unit = {},
    onRemoveFavourite: (packageName: String) -> Unit = {},
    onImportFavourites: () -> Unit = {},
    onExportFavourites: () -> Unit = {}
) {
    /*
     * For some reason paging3 frequently out-of-nowhere invalidates the list which causes
     * the loading animation to play again even if the keys are same causing a glitching effect.
     *
     * Save the initial loading state to make sure we don't replay the loading animation again.
     */
    var initialLoad by rememberSaveable { mutableStateOf(true) }
    var showInstallDialog by remember { mutableStateOf(false) }

    if (showInstallDialog) {
        InstallFavouritesDialog(
            count = favourites.itemCount,
            onConfirm = {
                showInstallDialog = false
                onInstallAll()
            },
            onDismiss = { showInstallDialog = false }
        )
    }

    @Composable
    fun SetupMenu() {
        FavouriteMenu(items = favourites.itemCount) { menuItem ->
            when (menuItem) {
                MenuItem.IMPORT -> onImportFavourites()
                MenuItem.EXPORT -> onExportFavourites()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_favourites_manager),
                actions = { SetupMenu() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(vertical = dimensionResource(R.dimen.spacing_medium))
        ) {
            when {
                favourites.loadState.refresh is LoadState.Loading && initialLoad -> {
                    ContainedLoadingIndicator()
                }

                else -> {
                    initialLoad = false

                    if (favourites.itemCount == 0) {
                        Placeholder(
                            modifier = Modifier.padding(paddingValues),
                            painter = painterResource(R.drawable.ic_favorite_unchecked),
                            message = stringResource(R.string.details_no_favourites)
                        )
                    } else {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState
                            ) {
                                item(key = "header") {
                                    SectionHeader(
                                        title = pluralStringResource(
                                            R.plurals.favourites_count,
                                            favourites.itemCount,
                                            favourites.itemCount
                                        ),
                                        trailing = if (showInstallAll) {
                                            {
                                                TextButton(
                                                    onClick = { showInstallDialog = true },
                                                    enabled = !isEnqueuing
                                                ) {
                                                    Text(
                                                        stringResource(R.string.action_install_all)
                                                    )
                                                }
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                }
                                items(
                                    count = favourites.itemCount,
                                    key = favourites.itemKey { it.packageName }
                                ) { index ->
                                    favourites[index]?.let { favourite ->
                                        FavouriteListItem(
                                            modifier = Modifier.animateItem(),
                                            favourite = favourite,
                                            download = downloads[favourite.packageName],
                                            onClick = {
                                                onNavigateTo(
                                                    Destination.AppDetails(favourite.packageName)
                                                )
                                            },
                                            onClear = { onRemoveFavourite(favourite.packageName) }
                                        )
                                    }
                                }
                            }
                            ScrollHint(
                                listState = listState,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun FavouriteScreenPreview(
    @PreviewParameter(FavouritePreviewProvider::class) favourite: Favourite
) {
    val favourites = List(10) {
        favourite.copy(packageName = "${favourite.packageName}.${Random.nextInt()}")
    }
    val flow = MutableStateFlow(PagingData.from(favourites)).collectAsLazyPagingItems()

    ScreenContent(favourites = flow)
}
