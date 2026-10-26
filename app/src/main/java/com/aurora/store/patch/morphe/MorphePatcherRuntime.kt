package com.aurora.store.patch.morphe

import android.content.Context
import android.os.Build
import android.util.Log
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkMerger
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchLoader
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

private data class AppVersion(
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val arch: String
)

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
            val patches = getLatestPatchDex() ?: return mapOf(false to moveTo(baseApkFile, outputFile))

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

    fun getCompatibleVersions(packageName: String): Map<Long, String> {
        val patchesDex = getLatestPatchDex() ?: return emptyMap()

        val compatibleVersions = patchesDex.mostCommonCompatibleVersions(setOf(packageName))[packageName]?.keys ?: return emptyMap()

        val client = OkHttpClient()

        return try {
            val jsonArray = JSONArray(client.getText("$VERSIONS_URL/$packageName.$JSON_EXTENSION"))

            val versions = List(jsonArray.length()) { i ->
                jsonArray.getJSONObject(i).let { item ->
                    AppVersion(
                        versionName = item.getString("versionName"),
                        versionCode = item.getLong("versionCode"),
                        minSdk = item.getInt("minSdk"),
                        arch = item.getString("arch")
                    )
                }
            }

            versions.filter { version ->
                version.versionName in compatibleVersions &&
                        Build.VERSION.SDK_INT >= version.minSdk &&
                        (version.arch in Build.SUPPORTED_64_BIT_ABIS || version.arch == "universal")
            }.associate { it.versionCode to it.versionName }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
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

    private fun getLatestPatchDex(): PatchLoader.Dex? {
        val patchBundle = getLatestPatchBundle() ?: return null
        val patchesDex = try {
            patchBundle.setReadOnly()
            loadPatchesFromDex(setOf(patchBundle), patchBundle.parentFile)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        } finally {
            patchBundle.setWritable(true)
        }
        return patchesDex
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
    }
}
