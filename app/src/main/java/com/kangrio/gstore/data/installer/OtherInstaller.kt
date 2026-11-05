package com.kangrio.gstore.data.installer

import android.content.Context
import android.content.Intent
import android.util.Log
import com.kangrio.gstore.GStoreApp
import com.kangrio.gstore.R
import com.kangrio.gstore.data.event.InstallerEvent
import com.kangrio.gstore.data.installer.base.InstallerBase
import com.kangrio.gstore.data.model.BuildType
import com.kangrio.gstore.data.model.Installer
import com.kangrio.gstore.data.model.InstallerInfo
import com.kangrio.gstore.data.room.download.Download
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.patch.ConstantsPatch
import com.kangrio.gstore.util.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtherInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) : InstallerBase(context) {

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        val installerInfo: InstallerInfo
            get() = InstallerInfo(
                id = 7,
                installer = Installer.OTHER,
                installerPackageNames = BuildType.PACKAGE_NAMES,
                title = R.string.pref_install_mode_other,
                subtitle = R.string.other_installer_subtitle,
                description = R.string.other_installer_desc
            )
    }

    val selectedPackageName: String?
        get() = Preferences.getString(context, ConstantsPatch.SELECT_OTHER_INSTALLER).takeIf { it.isNotEmpty() }

    override fun install(download: Download) {
        if (isAlreadyQueued(download.packageName)) {
            Log.i(TAG, "${download.packageName} already queued")
        } else {
            Log.i(TAG, "Received Other install request for ${download.packageName}")
            val fileList = mutableListOf<File>()
            fileList.addAll(getFiles(download.packageName, download.versionCode))
            install(fileList, download)
        }
    }

    private fun install(files: List<File>, download: Download) {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            setPackage(selectedPackageName)
            type = APK_MIME_TYPE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { getUri(it) }))
        }
        context.startActivity(intent)
        MainScope().launch {
            GStoreApp.enqueuedInstalls.remove(download.packageName)
            GStoreApp.events.send(
                InstallerEvent.Installed(
                    packageName = download.packageName
                )
            )
        }
    }
}