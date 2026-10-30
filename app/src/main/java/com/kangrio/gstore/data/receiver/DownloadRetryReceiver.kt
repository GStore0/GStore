/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.GStoreApp
import com.kangrio.gstore.data.helper.DownloadHelper
import com.kangrio.gstore.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-queues a previously failed download/install when the user taps "Retry" on its
 * (grouped) failure notification.
 */
@AndroidEntryPoint
class DownloadRetryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context, intent: Intent?) {
        val packageName = intent?.getStringExtra(DownloadHelper.PACKAGE_NAME) ?: ""

        if (packageName.isNotBlank()) {
            Log.d(TAG, "Received retry request for $packageName")
            // Clear the failure notification immediately for responsive feedback; the
            // download worker re-posts progress once it runs.
            NotificationUtil.clearAppNotification(context, packageName)
            GStoreApp.scope.launch(Dispatchers.IO) {
                downloadHelper.retryDownload(packageName)
            }
        }
    }
}
