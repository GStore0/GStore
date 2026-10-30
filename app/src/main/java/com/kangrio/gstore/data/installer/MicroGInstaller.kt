/*
 * SPDX-FileCopyrightText: 2021 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.gstore.data.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kangrio.gstore.Constants.PACKAGE_NAME_PLAY_STORE
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.R
import com.kangrio.gstore.data.activity.MicroGInstallerActivity
import com.kangrio.gstore.data.installer.base.InstallerBase
import com.kangrio.gstore.data.model.Installer
import com.kangrio.gstore.data.model.InstallerInfo
import com.kangrio.gstore.data.room.download.Download
import com.kangrio.gstore.util.PackageUtil.hasMicroGCompanion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MicroGInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 6,
                installer = Installer.MICROG,
                installerPackageNames = listOf(PACKAGE_NAME_PLAY_STORE),
                title = R.string.pref_install_mode_microg,
                subtitle = R.string.microg_installer_subtitle,
                description = R.string.microg_installer_desc
            )

        fun buildMicroGInstallIntent(uris: ArrayList<Uri>): Intent =
            Intent("org.microg.vending.action.INSTALL_PACKAGE").apply {
                setPackage(PACKAGE_NAME_PLAY_STORE)
                setType("application/vnd.android.package-archive")
                putExtra(Intent.EXTRA_STREAM, uris)
            }
    }

    override fun install(download: Download) {
        super.install(download)

        when {
            isAlreadyQueued(download.packageName) -> {
                Log.i(TAG, "${download.packageName} already queued")
            }

            hasMicroGCompanion(context) -> {
                Log.i(TAG, "Received microG install request for ${download.packageName}")

                val files = getFiles(download.packageName, download.versionCode)
                MicroGInstallerActivity.launch(context, download.packageName, files)

                Log.i(TAG, "Sent install request to microG installer for ${download.packageName}")
            }

            else -> {
                postError(
                    download.packageName,
                    context.getString(R.string.installer_status_failure),
                    context.getString(R.string.installer_microg_misconfigured)
                )
            }
        }
    }
}
