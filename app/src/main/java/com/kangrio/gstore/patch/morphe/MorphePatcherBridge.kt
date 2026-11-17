package com.kangrio.gstore.patch.morphe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.aurora.services.patch.IMorpheService
import com.aurora.services.patch.IProgressCallback
import com.kangrio.gstore.extensions.TAG
import com.kangrio.gstore.patch.morphe.Constant.RELEASE_URL
import com.kangrio.gstore.util.Preferences
import okhttp3.OkHttpClient
import java.io.File

class MorphePatcherBridge(
    val context: Context,
    val packageName: String
) {
    companion object {
        var morpheService: IMorpheService? = null
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                morpheService = IMorpheService.Stub.asInterface(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                morpheService = null
            }
        }

        private fun getService(context: Context): IMorpheService {
            if (morpheService == null) {
                context.bindService(Intent(context, MorpheService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
                while (morpheService == null) {
                    Thread.sleep(1000)
                }
            }
            return morpheService!!
        }

        private fun isPatchAvailable(patchFile: File): Boolean {
            return patchFile.exists() && patchFile.length() > 0
        }

        private fun getLatestPatchBundle(context: Context): File? {
            val patcherDir = File(context.cacheDir, "morphe").apply { mkdirs() }
            val patchFile = File(patcherDir, "patches.mpp")
            val currentVersion = Preferences.getString(context, "morphe_patch_version", "")

            if (isPatchAvailable(patchFile) && MorpheUtils.isLatestPatchAvailable(currentVersion).not()) {
                return patchFile
            }
            val client = OkHttpClient()

            return try {
                val releaseJson = client.getText(RELEASE_URL)
                val release = releaseJson.jsonToGitHubRelease()

                val asset = release.assets.firstOrNull {
                    it.name.endsWith(".mpp", ignoreCase = true)
                } ?: return null

                val patchBytes = client.getBytes(asset.browser_download_url)

                patchFile.parentFile?.mkdirs()
                patchFile.writeBytes(patchBytes)

                Log.d(TAG, "Patches downloaded: ${patchFile.absolutePath}")
                Preferences.putString(context, "morphe_patch_version", release.tag_name.substring(1))
                patchFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }

        fun patch(context: Context, packageName: String, apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {
            val bundleFile = getLatestPatchBundle(context) ?: return mapOf(false to outputFile)
            val currentVersion = Preferences.getString(context, "morphe_patch_version", "")
            val success = getService(context).startPatch(packageName, apksDir.absolutePath, outputFile.absolutePath, bundleFile.absolutePath, currentVersion, object : IProgressCallback.Stub() {
                override fun onProgress(progress: String) {
                    onProgress(progress)
                }
            })
            return mapOf(success to outputFile)
        }

        fun latestCompatibleVersion(packageName: String): Map<String, Long> {
            return MorpheUtils.getLatestCompatibleVersion(packageName)
        }
    }
}
