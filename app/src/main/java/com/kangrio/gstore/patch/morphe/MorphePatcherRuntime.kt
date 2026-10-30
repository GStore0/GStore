package com.kangrio.gstore.patch.morphe

import android.content.Context
import android.util.Log
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkMerger
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchLoader
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.loadPatchesFromDex
import app.morphe.patcher.patch.resourcePatch
import com.kangrio.gstore.patch.morphe.Constant.RELEASE_URL
import com.kangrio.gstore.util.Preferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.w3c.dom.Element
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

    fun morpheVersionPatch(patchVersion: String): ResourcePatch {
        return resourcePatch(default = true, name = "Morphe version") {
            execute {
                document("AndroidManifest.xml").use { document ->
                    val applicationNode = document
                        .getElementsByTagName("application")
                        .item(0) as Element

                    applicationNode.appendChild(
                        document.createElement("meta-data").apply {
                            setAttribute("android:name", "morphe_version")
                            setAttribute("android:value", patchVersion)
                        }

                    )
                }
            }
        }
    }

    fun applyPatch(apksDir: File, outputFile: File, onProgress: (String) -> Unit): Map<Boolean, File> {
        try {
            packageDir.deleteRecursively()

            onProgress("Merging APKs...")
            ApkMerger().merge(apksDir, baseApkFile)

            onProgress("Downloading patches...")
            val patches = getLatestPatchDex() ?: return mapOf(false to moveTo(baseApkFile, outputFile))

            onProgress("Checking for patches...")
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
        val currentVersion = Preferences.getString(context, "morphe_patch_version", "")
        if (isPatchAvailable() && MorpheUtils.isLatestPatchAvailable(currentVersion).not()) {
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
            val currentVersion = Preferences.getString(context, "morphe_patch_version", "")
            patcher += patches + morpheVersionPatch(currentVersion)

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

    companion object {
        private const val TAG = "MorphePatcher"
    }
}
