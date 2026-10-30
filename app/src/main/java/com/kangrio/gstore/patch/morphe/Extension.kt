package com.kangrio.gstore.patch.morphe

import com.kangrio.gstore.patch.data.Asset
import com.kangrio.gstore.patch.data.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


fun OkHttpClient.getText(url: String): String {
    return runBlocking(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "request")
            .build()

        newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Request failed: ${response.code}")
            }

            response.body.string()
        }
    }
}

fun OkHttpClient.getBytes(url: String): ByteArray {
    return runBlocking(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "request")
            .build()

        newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Download failed: ${response.code}")
            }

            response.body.bytes()
        }
    }
}

fun String.jsonToGitHubRelease(): GitHubRelease {
    val json = JSONObject(this)
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