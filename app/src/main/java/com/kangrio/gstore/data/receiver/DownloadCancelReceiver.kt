/*
 * SPDX-FileCopyrightText: 2025 Aurora OSS
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadCancelReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadHelper: DownloadHelper

    override fun onReceive(context: Context, intent: Intent?) {
        val packageName: String = intent?.getStringExtra("PACKAGE_NAME") ?: ""

        if (packageName.isNotBlank()) {
            Log.d(TAG, "Received cancel download request for $packageName")
            GStoreApp.scope.launch(Dispatchers.IO) {
                downloadHelper.cancelDownload(packageName)
            }
        }
    }
}
