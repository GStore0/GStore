package com.kangrio.gstore.patch.morphe

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.aurora.services.patch.IMorpheService
import com.aurora.services.patch.IProgressCallback
import java.io.File

class MorpheService : Service() {
    val binder = object : IMorpheService.Stub() {
        override fun startPatch(packageName: String, apksDir: String, outputFile : String, callback: IProgressCallback): Boolean {
            val apksDir = File(apksDir)
            val outputFile = File(outputFile)

            val morpher = MorphePatcherRuntime(this@MorpheService, packageName)
            val result = morpher.patch(apksDir, outputFile) {
                callback.onProgress(it)
            }
            return result.keys.first()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand, intent=$intent")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val TAG = "MorpheService"
    }
}