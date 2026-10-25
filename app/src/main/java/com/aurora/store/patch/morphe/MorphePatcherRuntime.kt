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
import app.morphe.patcher.patch.mostCommonCompatibleVersions
import com.aurora.store.patch.data.Asset
import com.aurora.store.patch.data.GitHubRelease
import com.aurora.store.util.Preferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MorphePatcherRuntime(
    val context: Context,
    val packageName: String
) {
    private val patcherDir = File(context.cacheDir, "morphe").apply { mkdirs() }
    private val patchFile = File(patcherDir, "patches.mpp")
    private val packageDir = File(patcherDir, packageName).apply { mkdirs() }
    private val workDir = File(packageDir, "patch_${System.currentTimeMillis()}").apply { mkdirs() }
    private val baseApkFile = File(packageDir, "original.apk").apply { delete() }

    fun moveTo(from: File, destination: File): File {
        from.renameTo(destination)
        return destination
    }

    fun patch(apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {

        var result = mapOf<Boolean, File>()
        runBlocking {
            result = applyPatch(apksDir, outputFile, onProgress)
        }

        return result
    }

    fun applyPatch(apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {
        try {
            packageDir.deleteRecursively()

            onProgress("Merging APKs...")
            ApkMerger().merge(apksDir, baseApkFile)

            onProgress("Checking for patches...")
            val patchBundle = getLatestPatchBundle()
                ?: return mapOf(false to moveTo(baseApkFile, outputFile))

            val patches = try {
                patchBundle.setReadOnly()
                loadPatchesFromDex(setOf(patchBundle), patchBundle.parentFile)
            } catch (e: Throwable) {
                e.printStackTrace()
                return mapOf(false to moveTo(baseApkFile, outputFile))
            } finally {
                patchBundle.setWritable(true)
            }


            val selectedPatches = selectPatches(
                patches = patches,
                packageName = packageName,
                versionName = null,
            )

            if (selectedPatches.isEmpty()) {
                return mapOf(false to moveTo(baseApkFile, outputFile))
            }

            return runCatching {
                applyPatches(
                    patches = selectedPatches,
                    workDir = workDir,
                    outputFile = outputFile,
                    onProgress
                )

                mapOf(true to outputFile)
            }.onFailure {
                it.printStackTrace()
                moveTo(baseApkFile, outputFile)
            }.getOrDefault(mapOf(false to outputFile))
        } finally {
            packageDir.deleteRecursively()
        }
    }

    private fun getCompatibleVersions(packageName: String): Map<Long, String> {
        val patchBundle = getLatestPatchBundle() ?: return emptyMap()
        val patchesDex = loadPatchesFromDex(setOf(patchBundle))

        val mostCommonCompatibleVersions = patchesDex.mostCommonCompatibleVersions(setOf(packageName))
        val client = OkHttpClient()
        try {
            val versionsJson = client.getText("$VERSIONS_URL/$packageName.$JSON_EXTENSION")
            val jsonArray = JSONArray(versionsJson)
            val versions = mutableMapOf<Long, String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                val versionName = item.getString("versionName")
                val versionCode = item.getLong("versionCode")
                versions[versionCode] = versionName
            }

            val filteredVersions = versions.filter {
                it.value in (mostCommonCompatibleVersions[packageName]?.keys ?: emptySet())
            }

            return filteredVersions
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun isLatestPatchAvailable(): Boolean {
        val currentVersion = Preferences.getString(context, "morphe_patch_version", "").takeIf { it.isNotEmpty() } ?: return true
        val latestVersion = getLatestPatchVersion()
        return currentVersion.compareTo(latestVersion) < 0
    }

    private fun getLatestPatchVersion(): String {
        val client = OkHttpClient()
        try {
            val releaseJson = client.getText(RELEASE_URL)
            val release = parseRelease(releaseJson)
            return release.tag_name.substring(1)
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun isPatchAvailable(): Boolean {
        return patchFile.exists() && patchFile.length() > 0
    }

    private fun getLatestPatchBundle(): File? {
        if (isPatchAvailable() && isLatestPatchAvailable().not()) {
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
            Preferences.putString(context, "morphe_patch_version", release.tag_name.substring(1))
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
        outputFile: File,
        onProgress: (String) -> Unit
    ) {
        onProgress("Preparing...")
        workDir.mkdirs()
        outputFile.delete()

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
                        Log.i(TAG, "Applying: \"${result.patch}\" failed:\n${result.exception}")
                        onProgress("Applying: ${result.patch} failed, ${result.exception?.message}")
                    } else {
                        Log.i(TAG, "Applying: \"${result.patch}\" succeeded")
                        onProgress("Applying: ${result.patch} succeeded")
                    }
                }
            }
            onProgress("Repacking...")
            patcher.get()
        }

        result.applyTo(outputFile)

        Log.d(
            TAG,
            "Patch finished: ${result.dexFiles}, ${result.resources}, ${System.currentTimeMillis() - startTime}ms"
        )
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
        private const val VERSIONS_URL = "https://raw.githubusercontent.com/kangrio/morphe-app-database/refs/heads/main"
        private const val JSON_EXTENSION = "json"

        fun latestCompatibleVersion(context: Context, packageName: String): Map<String, Long> {
            val sorted = MorphePatcherRuntime(context, packageName).getCompatibleVersions(packageName).toSortedMap()
            if (sorted.isEmpty()) return emptyMap()
            return mapOf(sorted.values.last() to sorted.keys.last())
        }
    }
}
