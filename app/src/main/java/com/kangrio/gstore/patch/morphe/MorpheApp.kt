package com.kangrio.gstore.patch.morphe

import android.content.Context
import com.kangrio.gstore.patch.ConstantsPatch.MORPHE_PATCHES_TAGS_URL
import java.net.HttpURLConnection
import java.net.URL

object MorpheApp {
    enum class App(
        val packageName: String,
        val path: String
    ) {
        YOUTUBE(
            "com.google.android.youtube",
            "patches/src/main/kotlin/app/morphe/patches/youtube/shared/Constants.kt"
        ),
        YOUTUBE_MUSIC(
            "com.google.android.apps.youtube.music",
            "patches/src/main/kotlin/app/morphe/patches/music/shared/Constants.kt"
        );

        companion object {
            fun fromPackageName(packageName: String): App =
                entries.first { it.packageName == packageName }
        }
    }

    data class Target(
        val version: String,
        val minSdk: Int?,
        val isExperimental: Boolean,
        val app: App
    )

    val apps = mapOf(
        App.YOUTUBE.packageName to "Youtube",
        App.YOUTUBE_MUSIC.packageName to "YTMusic"
    )

    fun get(context: Context, app: App): List<Target> {
        val latestPatchVersion = MorpheUtils.getLatestStorePatchVersion(context)
        val source = download("${MORPHE_PATCHES_TAGS_URL}/v$latestPatchVersion/${app.path}")

        // Match each AppTarget(...) block, including multiline arguments.
        val targetPattern = Regex(
            """AppTarget\s*\((.*?)\)""",
            RegexOption.DOT_MATCHES_ALL
        )

        return targetPattern.findAll(source).mapNotNull { match ->
            val arguments = match.groupValues[1]

            val version = Regex("""\bversion\s*=\s*"([^"]+)"""")
                .find(arguments)
                ?.groupValues
                ?.get(1)
                ?: return@mapNotNull null

            val minSdk = Regex("""\bminSdk\s*=\s*(\d+)""")
                .find(arguments)
                ?.groupValues
                ?.get(1)
                ?.toInt()

            val isExperimental = Regex(
                """\bisExperimental\s*=\s*true\b"""
            ).containsMatchIn(arguments)

            Target(
                version = version,
                minSdk = minSdk,
                isExperimental = isExperimental,
                app = app
            )
        }.toList()
    }

    fun get(context: Context, packageName: String): List<Target> =
        get(context, App.fromPackageName(packageName))

    fun getAll(context: Context): List<Target> =
        App.entries.flatMap{
            get(context, it)
        }

    fun getLatestVersion(
        context: Context,
        app: App,
        includeExperimental: Boolean = false
    ): String? = get(context, app)
        .firstOrNull { includeExperimental || !it.isExperimental }
        ?.version

    fun getLatestVersion(
        context: Context,
        packageName: String,
        includeExperimental: Boolean = false
    ): String? = getLatestVersion(context, App.fromPackageName(packageName), includeExperimental)

    fun getAllVersion(
        context: Context,
        app: App,
        includeExperimental: Boolean = false
    ): List<String> = get(context, app).mapNotNull {
        if (includeExperimental || !it.isExperimental) it.version else null
    }

    fun getAllVersion(
        context: Context,
        packageName: String,
        includeExperimental: Boolean = false
    ): List<String> = getAllVersion(context, App.fromPackageName(packageName), includeExperimental)

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection

        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "MorpheApp")

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}