package com.kangrio.gstore.patch.morphe

import android.os.Build
import com.kangrio.gstore.patch.data.Asset
import com.kangrio.gstore.patch.data.GitHubRelease
import com.kangrio.gstore.patch.morphe.Constant.RELEASE_URL
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.json.JSONObject

object MorpheUtils {
    fun getLatestPatchVersion(): String {
        val client = OkHttpClient()
        try {
            val releaseJson = client.getText(RELEASE_URL)
            val release = parseRelease(releaseJson)
            return release.tag_name.substring(1)
        } catch (e: Throwable) {
            e.printStackTrace()
            return ""
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    fun isLatestPatchAvailable(currentVersion: String): Boolean {
        val latestVersion = getLatestPatchVersion()
        return currentVersion.compareTo(latestVersion) < 0
    }

    fun getLatestCompatibleVersion(packageName: String): Map<String, Long> {
        val apps = mapOf(
            "com.google.android.youtube" to "youtube",
            "com.google.android.apps.youtube.music" to "youtube-music",
        )
        val appName = apps[packageName] ?: return emptyMap()
        return try {
            val latestVersionName = MorpheApp.getLatestVersion(packageName, false) ?: return emptyMap()
            val latestVersionCode = runBlocking {
                ApkMirror.getVersionCode("google-inc", appName, latestVersionName, "universal", Build.SUPPORTED_64_BIT_ABIS.first())
            }

            latestVersionCode?.let { mapOf(latestVersionName to latestVersionCode) } ?: emptyMap()
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyMap()
        }
    }

    fun parseRelease(jsonText: String): GitHubRelease {
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
}