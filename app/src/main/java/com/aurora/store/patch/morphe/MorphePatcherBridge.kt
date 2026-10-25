package com.aurora.store.patch.morphe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.io.File


class ResultReceiver(val onResult: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != MorpheService.ACTION_RESULT) return
        val success = intent.getBooleanExtra(MorpheService.EXTRA_SUCCESS, false)
        onResult(success)
    }
}

class ProgressReceiver(val onProgress: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != MorpheService.ACTION_PROGRESS) return
        val progress = intent.getStringExtra(MorpheService.EXTRA_PROGRESS)
        onProgress(progress ?: "")
    }
}

class MorphePatcherBridge(
    val context: Context,
    val packageName: String
) {
    fun patch(apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {
        val intent = Intent(context, MorpheService::class.java)
        intent.action = MorpheService.ACTION_PATCH
        intent.putExtra(MorpheService.EXTRA_PACKAGE_NAME, packageName)
        intent.putExtra(MorpheService.EXTRA_APKS_DIR, apksDir.absolutePath)
        intent.putExtra(MorpheService.EXTRA_OUTPUT_FILE, outputFile.absolutePath)
        context.startService(intent)

        var success = false
        var wait = true
        val progressReceiver = ProgressReceiver(onProgress)
        val resultReceiver = ResultReceiver { success = it; wait = false }
        registerForResult(progressReceiver, resultReceiver)
        while (wait) {
            Thread.sleep(1000)
        }
        context.unregisterReceiver(progressReceiver)
        context.unregisterReceiver(resultReceiver)

        return mapOf(success to outputFile)
    }

    fun registerForResult(progressReceiver: ProgressReceiver, resultReceiver: ResultReceiver) {
        val progressIntentFilter = IntentFilter(MorpheService.ACTION_PROGRESS)
        val resultIntentFilter = IntentFilter(MorpheService.ACTION_RESULT)
        ContextCompat.registerReceiver(context, progressReceiver, progressIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, resultReceiver, resultIntentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
