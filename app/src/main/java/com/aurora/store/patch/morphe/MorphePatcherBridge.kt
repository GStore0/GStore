package com.aurora.store.patch.morphe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.aurora.services.patch.IMorpheService
import com.aurora.services.patch.IProgressCallback
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

        fun patch(context: Context, packageName: String, apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {
            val success = getService(context).startPatch(packageName, apksDir.absolutePath, outputFile.absolutePath, object : IProgressCallback.Stub() {
                override fun onProgress(progress: String) {
                    onProgress(progress)
                }
            })
            return mapOf(success to outputFile)
        }

        fun latestCompatibleVersion(context: Context, packageName: String): Map<String, Long> {
            val result = getService(context).getCompatibleVersions(packageName) as Map<Long, String>

            val sorted = result.toSortedMap()
            if (sorted.isEmpty()) return emptyMap()
            return mapOf(sorted.values.last() to sorted.keys.last())
        }
    }
}
