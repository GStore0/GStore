package com.kangrio.gstore.patch.morphe

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object ApkMirror {

    private const val BASE_URL = "https://www.apkmirror.com/apk"

    suspend fun getVersionCode(
        org: String,
        appName: String,
        versionName: String,
        arch: String = "universal",
        fallbackArch: List<String>? = null,
        minSdk: Int = 0
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val orgSlug = slug(org)
            val appSlug = slug(appName)
            val versionSlug = slug(versionName)

            val url = "$BASE_URL/$orgSlug/$appSlug/" +
                    "$appSlug-$versionSlug-release/"

            val doc = Jsoup.connect(url)
                .userAgent("APKUpdater-v0.0.604")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9")
                .referrer("https://www.apkmirror.com/")
                .timeout(30_000)
                .get()

            val rows = doc.select(".variants-table .table-row")

            fun findCode(targetArch: List<String>): Long? {
                for (row in rows) {
                    val cells = row.select(".table-cell")
                    if (cells.size < 4) continue

                    val releaseCell = cells[0]
                    val rowVersion = releaseCell.selectFirst("a")
                        ?.text()?.trim()

                    if (rowVersion != versionName) continue

                    val rowArch = cells[1].text().trim()
                    for (arch in targetArch) {
                        if (!matchesArch(rowArch, arch)) continue
                    }

                    val androidVersion = cells[2].text().trim()
                    val requiredSdk = androidVersion
                        .substringAfter("Android ", "")
                        .removeSuffix("+")
                        .takeIf { it.isNotBlank() }
                        ?.let { versionToSdk(it) }
                        ?: continue

                    if (minSdk > 0 && requiredSdk > minSdk) continue

                    val code = Regex("""\b\d{5,12}\b""")
                        .find(releaseCell.text())
                        ?.value
                        ?.toLongOrNull()

                    if (code != null) return code
                }

                return null
            }

            findCode(listOf(arch))
                ?: fallbackArch
                    ?.takeIf { arch !in it }
                    ?.let { findCode(it) }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun matchesArch(
        actual: String,
        requested: String
    ): Boolean {
        if (requested.equals("universal", true)) {
            return actual.equals("universal", true) ||
                    actual.equals("noarch", true)
        }

        val aliases = mapOf(
            "arm64-v8a" to setOf("arm64-v8a", "arm64", "aarch64"),
            "armeabi-v7a" to setOf("armeabi-v7a", "armeabi", "arm"),
            "x86" to setOf("x86"),
            "x86_64" to setOf("x86_64", "x64", "amd64")
        )

        val normalized = requested.lowercase()
        val accepted = aliases[normalized] ?: setOf(normalized)

        return actual.lowercase() in accepted
    }

    private fun versionToSdk(version: String): Int {
        val parts = version.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return -1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0

        return when {
            major < 8 -> -1
            major == 8 -> 26
            major == 9 -> 28
            major == 10 -> 29
            major == 11 -> 30
            major == 12 && minor == 0 -> 31
            major == 12 -> 32
            major == 13 -> 33
            major == 14 -> 34
            major == 15 -> 35
            major == 16 -> 36
            else -> -1
        }
    }

    private fun slug(value: String): String =
        value.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
