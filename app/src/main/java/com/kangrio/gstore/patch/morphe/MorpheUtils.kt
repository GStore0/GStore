package com.kangrio.gstore.patch.morphe

import android.content.Context
import android.os.Build
import com.aurora.gplayapi.data.serializers.LocaleSerializer
import com.aurora.gplayapi.data.serializers.PropertiesSerializer
import com.kangrio.gstore.patch.ConstantsPatch.MORPHE_PATCHES_RELEASE_URL
import com.kangrio.gstore.patch.ConstantsPatch.MORPHE_PATCHES_TAGS_URL
import com.kangrio.gstore.patch.ConstantsPatch.PREFERENCE_MORPHE_LATEST_PATCH
import com.kangrio.gstore.patch.data.Asset
import com.kangrio.gstore.patch.data.GitHubRelease
import com.kangrio.gstore.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.tomlj.Toml

object MorpheUtils {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule = SerializersModule {
            contextual(LocaleSerializer)
            contextual(PropertiesSerializer)
        }
        explicitNulls = false
    }

    fun getPatchReleases(): List<GitHubRelease> {
        val client = OkHttpClient()

        return try {
            val releaseJson = client.getText("${MORPHE_PATCHES_RELEASE_URL}?per_page=100")
            json.decodeFromString<List<GitHubRelease>>(releaseJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    suspend fun storeLatestPatch(context: Context) = withContext(Dispatchers.IO) {
        val latestPatch = getLatestPatchVersion()
        Preferences.putString(context, PREFERENCE_MORPHE_LATEST_PATCH, latestPatch)
    }

    fun getLatestStorePatchVersion(context: Context): String {
        return Preferences.getString(context, PREFERENCE_MORPHE_LATEST_PATCH, "")
    }

    private fun getLatestPatchVersion(): String {
        val patchReleases = getPatchReleases()
        val release = patchReleases.firstOrNull {
            val version = it.tag_name.substring(1)
            !it.prerelease && isCompatible(version)
        }
        val version = release?.tag_name?.substring(1) ?: ""
        return version
    }

    fun getPatcherVersion(patchVersion: String): String {
        val client = OkHttpClient()
        try {
            val tomlString = client.getText("${MORPHE_PATCHES_TAGS_URL}/v$patchVersion/gradle/libs.versions.toml")
            val toml = Toml.parse(tomlString)
            val versions = toml.getTable("versions") ?: return ""
            return versions.getString("morphe-patcher") ?: ""
        } catch (e: Throwable) {
            e.printStackTrace()
            return ""
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }

    fun isCompatible(patchVersion: String): Boolean {
        val patcherVersion = getPatcherVersion(patchVersion)
        return com.kangrio.morphe.BuildConfig.MORPHE_PATCHER_VERSION_NAME.compareTo(patcherVersion) >= 0
    }

    fun isLatestPatchAvailable(context: Context, currentVersion: String): Boolean {
        val latestVersion = getLatestStorePatchVersion(context)
        return currentVersion.compareTo(latestVersion) < 0
    }

    fun getLatestCompatibleVersion(context: Context, packageName: String): Map<String, Long> {
        val apps = mapOf(
            "com.google.android.youtube" to "youtube",
            "com.google.android.apps.youtube.music" to "youtube-music",
        )
        val appName = apps[packageName] ?: return emptyMap()
        return try {
            val supportedVersions = MorpheApp.getAllVersion(context, packageName, false)
            var latestVersionName = supportedVersions.first()

            val latestVersionCode = supportedVersions.firstNotNullOfOrNull { versionName ->
                runBlocking {
                    latestVersionName = versionName
                    ApkMirror.getVersionCode("google-inc", appName, versionName, "universal", listOf(Build.SUPPORTED_64_BIT_ABIS.first()))
                }
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