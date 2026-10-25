package com.aurora.store.patch.morphe

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.aurora.store.BuildConfig
import java.io.File

class MorpheService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand, intent=$intent")
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }

        val action = intent.action
        if (action != ACTION_PATCH) { stopSelf(); return START_NOT_STICKY; }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run { stopSelf(); return START_NOT_STICKY; }
        val apksDirPath = intent.getStringExtra(EXTRA_APKS_DIR) ?: run { stopSelf(); return START_NOT_STICKY; }
        val outputFilePath = intent.getStringExtra(EXTRA_OUTPUT_FILE) ?: run { stopSelf(); return START_NOT_STICKY; }

        val apksDir = File(apksDirPath)
        val outputFile = File(outputFilePath)

        val morpher = MorphePatcherRuntime(this@MorpheService, packageName)
        val result = morpher.patch(apksDir, outputFile) {
            val intent = Intent(ACTION_PROGRESS).apply {
                setPackage(BuildConfig.APPLICATION_ID)
                putExtra(EXTRA_PROGRESS, it)
            }
            sendBroadcast(intent)
        }
        val success = result.keys.first()
        val intent = Intent(ACTION_RESULT).apply {
            setPackage(BuildConfig.APPLICATION_ID)
            putExtra(EXTRA_SUCCESS, success)
        }
        sendBroadcast(intent)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "MorpheService"
        const val ACTION_PATCH = "morphe_patch"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_APKS_DIR = "apksDir"
        const val EXTRA_OUTPUT_FILE = "outputFile"
        const val ACTION_PROGRESS = "morphe_progress"
        const val EXTRA_PROGRESS = "progress"
        const val ACTION_RESULT = "morphe_result"
        const val EXTRA_SUCCESS = "success"
    }
}