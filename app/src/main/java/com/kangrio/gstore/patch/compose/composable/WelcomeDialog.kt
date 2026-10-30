package com.kangrio.gstore.patch.compose.composable

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kangrio.gstore.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WelcomeDialog {
    private const val DEFAULT_CONTENT = """
        {
          "updateTs": 1793352456,
          "content": "<p>This build is a community fork adapted to run Android (AOSP). Before you continue, please read where this app comes from and what it does.</p><br><p><b>Source</b></p><p>This fork: GStore, maintained by KangRio<br>Upstream: Aurora Store, by whyorean (AuroraOSS)<br>License: GPLv3, free and open source software</p><p>&#8226; <a href=\"https://github.com/GStore0/GStore\">This fork on GitHub</a><br>&#8226; <a href=\"https://github.com/whyorean/AuroraStore\">Original Aurora Store</a><br>&#8226; <a href=\"https://auroraoss.com/\">Official site</a></p><br><p><b>This Build</b></p><p>Packaged and signed independently for AOSP, with rootless microG/GmsCore patching so apps run without root access. See the <a href=\"https://github.com/GStore0/GStore#readme\">README</a> for build-specific notes and known limitations on this platform.</p><br><p><b>Support</b></p><p>If you find this build useful, you can support its development and help keep the project alive.<br>&#8226; <a href=\"https://ko-fi.com/kangrio\">ko-fi.com/kangrio</a></p>"
        }
    """
    private const val WELCOME_URL = "https://raw.githubusercontent.com/GStore0/GStore/welcome/welcome.json"

    private const val PREF_FILE = "welcome_dialog"
    private const val KEY_SEEN_TS = "seen_update_ts"
    private const val KEY_CACHED_TS = "cached_update_ts"
    private const val KEY_CACHED_CONTENT = "cached_content"

    private data class Welcome(val updateTs: Long, val content: String)

    // Survives recomposition / config changes, resets on process death
    private var dismissedThisSession = false

    @Composable
    fun Dialog() {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE) }

        var welcome by remember { mutableStateOf<Welcome?>(null) }
        var dismissed by remember { mutableStateOf(dismissedThisSession) }

        LaunchedEffect(Unit) {
            if (dismissedThisSession) return@LaunchedEffect
            welcome = withContext(Dispatchers.IO) {
                fetchWelcome()?.also {
                    prefs.edit()
                        .putLong(KEY_CACHED_TS, it.updateTs)
                        .putString(KEY_CACHED_CONTENT, it.content)
                        .apply()
                } ?: run {
                    // Network failed -> fall back to the last cached copy
                    Log.d("WelcomeDialog", "Failed to fetch welcome from $WELCOME_URL")
                    val ts = prefs.getLong(KEY_CACHED_TS, 0L)
                    val c = prefs.getString(KEY_CACHED_CONTENT, null)
                    if (ts > 0L && c != null) {
                        Welcome(ts, c)
                    } else {
                        val json = JSONObject(DEFAULT_CONTENT)
                        val updateTs = json.optLong("updateTs", 0L)
                        val content = json.optString("content")
                        if (updateTs <= 0L || content.isBlank()) null
                        else Welcome(updateTs, content)
                    }
                }
            }
        }

        val data = welcome ?: return
        if (dismissed) return
        // Already dismissed this timestamp (or a newer one) -> stay hidden
        if (data.updateTs <= prefs.getLong(KEY_SEEN_TS, 0L)) return

        var dontShowAgain by remember { mutableStateOf(false) }

        val linkColor = MaterialTheme.colorScheme.primary
        val html = remember(data.content, linkColor) {
            AnnotatedString.fromHtml(
                htmlString = data.content,
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    )
                ),
            )
        }

        BasicAlertDialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Welcome to GStore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Only the content scrolls; header and buttons stay pinned
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = html, // links are clickable and open via LocalUriHandler
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                        )
                        Text(
                            text = "Don't show this again",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (dontShowAgain) {
                                prefs.edit().putLong(KEY_SEEN_TS, data.updateTs).apply()
                            }
                            dismissedThisSession = true
                            dismissed = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Got it, continue")
                    }
                }
            }
        }
    }

    /** Blocking; call from Dispatchers.IO. Returns null on any failure. */
    private fun fetchWelcome(): Welcome? = try {
        val conn = (URL(WELCOME_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val updateTs = json.optLong("updateTs", 0L)
                val content = json.optString("content")
                if (updateTs <= 0L || content.isBlank()) null
                else Welcome(updateTs, content)
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}