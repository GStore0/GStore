package com.kangrio.gstore.patch.morphe

import java.net.HttpURLConnection
import java.net.URL

object MorpheApp {

    private const val BASE_URL = "https://raw.githubusercontent.com/MorpheApp/morphe-patches/main/patches/src/main/kotlin/app/morphe/patches"

    enum class App(
        val packageName: String,
        val path: String
    ) {
        YOUTUBE(
            "com.google.android.youtube",
            "youtube/shared/Constants.kt"
        ),
        YOUTUBE_MUSIC(
            "com.google.android.apps.youtube.music",
            "music/shared/Constants.kt"
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

    fun get(app: App): List<Target> {
        val source = download("$BASE_URL/${app.path}")

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

    fun get(packageName: String): List<Target> =
        App.fromPackageName(packageName).let(::get)

    fun getAll(): List<Target> =
        App.entries.flatMap(::get)

    fun getLatestVersion(
        app: App,
        includeExperimental: Boolean = false
    ): String? = get(app)
        .firstOrNull { includeExperimental || !it.isExperimental }
        ?.version

    fun getLatestVersion(
        packageName: String,
        includeExperimental: Boolean = false
    ): String? = getLatestVersion(App.fromPackageName(packageName), includeExperimental)

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