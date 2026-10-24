package com.aurora.store.patch.morphe

import android.content.Context
import android.util.Log
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkMerger
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.loadPatchesFromDex
import com.aurora.store.patch.data.Asset
import com.aurora.store.patch.data.GitHubRelease
import com.reandroid.apk.ApkModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

class MorphePatcher(
    context: Context
) {
    private val patcherDir = File(context.cacheDir, "patcher").apply { mkdirs() }
    private val patchFile = File(patcherDir, "patches.mpp")
    private val outputFile = File(patcherDir, "output.apk").apply { delete() }
    private val baseApkFile = File(patcherDir, "original.apk").apply { delete() }
    private val workDir = File(patcherDir, "patch_${System.currentTimeMillis()}").apply { mkdirs() }

    fun patch(apksDir: File): File? {
        baseApkFile.delete()
        ApkMerger().merge(apksDir, baseApkFile)

        val patchBundle = downloadLatestPatchBundle()
            ?: return null

        val patches = loadPatchesFromDex(setOf(patchBundle))
        val apkModule = ApkModule.loadApkFile(baseApkFile)

        val selectedPatches = selectPatches(
            patches = patches,
            packageName = apkModule.packageName,
            versionName = null,
        )

        if (selectedPatches.isEmpty()) {
            return null
        }

        return runCatching {
            applyPatches(
                patches = selectedPatches,
                workDir = workDir,
                outputFile = outputFile
            )

            outputFile
        }.onFailure {
            it.printStackTrace()
        }.getOrDefault(null)
    }

    private fun downloadLatestPatchBundle(): File? {
        if (patchFile.exists() && patchFile.length() > 0) {
            return patchFile
        }
        val client = OkHttpClient()

        return try {
            val releaseJson = client.getText(RELEASE_URL)
            val release = parseRelease(releaseJson)

            val asset = release.assets.firstOrNull {
                it.name.endsWith(".mpp", ignoreCase = true)
            } ?: return null

            val patchBytes = client.getBytes(asset.browser_download_url)

            patchFile.parentFile?.mkdirs()
            patchFile.writeBytes(patchBytes)

            Log.d(TAG, "Patches downloaded: ${patchFile.absolutePath}")
            patchFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun parseRelease(jsonText: String): GitHubRelease {
        val json = JSONObject(jsonText)
        val assetsJson = json.getJSONArray("assets")

        val assets = buildList {
            for (i in 0 until assetsJson.length()) {
                val item = assetsJson.getJSONObject(i)

                add(
                    Asset(
                        name = item.getString("name"),
                        browser_download_url = item.getString("browser_download_url"),
                        size = item.getLong("size"),
                        digest = item.optString("digest")
                    )
                )
            }
        }

        return GitHubRelease(
            tag_name = json.getString("tag_name"),
            name = json.getString("name"),
            body = json.optString("body"),
            assets = assets,
            published_at = json.getString("published_at")
        )
    }

    private fun findAndCopyBaseApk(apkFiles: List<File>): ApkModule? {
        for (file in apkFiles) {
            val module = ApkModule.loadApkFile(file)

            if (module.isBaseModule) {
                baseApkFile.delete()
                file.copyTo(baseApkFile, overwrite = true)
                return module
            }
        }

        return null
    }

    private fun selectPatches(
        patches: Set<Patch<*>>,
        packageName: String,
        versionName: String?
    ): Set<Patch<*>> {
        return patches
            .asSequence()
            .filter { patch ->
                patch.compatibility?.any { compatibility ->
                    compatibility.packageName == packageName &&
                            compatibility.targets.any { target ->
                                if (versionName == null) true else target.version == versionName
                            }
                } ?: false
            }
            .filterNot { patch ->
                patch.name?.contains("GmsCore", ignoreCase = true) == true
            }
            .filterNot { patch ->
                patch.name?.contains("Custom branding", ignoreCase = true) == true
            }
            .toSet()
    }

    private fun applyPatches(
        patches: Set<Patch<*>>,
        workDir: File,
        outputFile: File
    ) {
        workDir.mkdirs()
        outputFile.delete()

        try {
            val startTime = System.currentTimeMillis()

            val result = Patcher(
                PatcherConfig(
                    apkFile = baseApkFile,
                    temporaryFilesPath = workDir,
                    useBytecodeMode = BytecodeMode.STRIP_FAST
                )
            ).use { patcher ->
                patcher += patches

                runBlocking {
                    patcher().collect { result ->
                        if (result.exception != null) {
                            Log.i(TAG, "Apply: \"${result.patch}\" failed:\n${result.exception}")
                        } else {
                            Log.i(TAG, "Apply: \"${result.patch}\" succeeded")
                        }
                    }
                }

                patcher.get()
            }

            result.applyTo(outputFile)

            Log.d(
                TAG,
                "Patch finished: ${result.dexFiles}, ${result.resources}, ${System.currentTimeMillis() - startTime}ms"
            )
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun OkHttpClient.getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "request")
            .build()

        return newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Request failed: ${response.code}")
            }

            response.body.string()
        }
    }

    private fun OkHttpClient.getBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "request")
            .build()

        return newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Download failed: ${response.code}")
            }

            response.body.bytes()
        }
    }

    companion object {
        private const val TAG = "MorphePatcher"
        private const val RELEASE_URL =
            "https://api.github.com/repos/MorpheApp/morphe-patches/releases/latest"
    }
}
