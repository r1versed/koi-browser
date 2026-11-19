package dev.sk2andy.materialbrowser.browser.gecko

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

internal data class KoiDetectedMedia(
    val url: String,
    val mime: String,
    val sizeBytes: Long,
    val isStream: Boolean,
    val pageUrl: String,
)

/** Videos found per site (host), newest first. Main thread only. */
internal object KoiMediaStore {
    private const val TAG = "KoiMedia"
    private const val MAX_PER_HOST = 25
    private const val MAX_HOSTS = 30

    private var version by mutableStateOf(0)
    private val byHost = LinkedHashMap<String, MutableList<KoiDetectedMedia>>()

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching { Uri.parse(url).host }.getOrNull()?.lowercase()?.removePrefix("www.")
    }

    fun itemsFor(pageUrl: String?): List<KoiDetectedMedia> {
        @Suppress("UNUSED_VARIABLE")
        val observed = version
        val host = hostOf(pageUrl) ?: return emptyList()
        return byHost[host]?.toList() ?: emptyList()
    }

    fun handle(message: JSONObject) {
        val topUrl = message.optString("topUrl")
        val host = hostOf(topUrl) ?: return
        when (message.optString("type")) {
            "reset" -> {
                if (byHost.remove(host) != null) version++
            }
            "media" -> {
                val url = message.optString("url")
                if (!url.startsWith("http", ignoreCase = true)) return
                val item = KoiDetectedMedia(
                    url = url,
                    mime = message.optString("mime"),
                    sizeBytes = message.optLong("size", -1L),
                    isStream = message.optBoolean("stream", false),
                    pageUrl = topUrl,
                )
                val list = byHost.getOrPut(host) { mutableListOf() }
                val existing = list.indexOfFirst { it.url == url }
                if (existing >= 0) {
                    if (item.sizeBytes > list[existing].sizeBytes) list[existing] = item
                } else {
                    list.add(0, item)
                    while (list.size > MAX_PER_HOST) list.removeAt(list.lastIndex)
                }
                while (byHost.size > MAX_HOSTS) byHost.remove(byHost.keys.first())
                Log.d(TAG, "detected " + item.url)
                version++
            }
        }
    }

    fun fileNameFor(item: KoiDetectedMedia): String {
        val last = runCatching { Uri.parse(item.url).lastPathSegment }.getOrNull().orEmpty()
        var base = last.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim('_', '.', ' ').take(80)
        if (base.isEmpty()) base = "video"
        val known = Regex("\\.(mp4|m4v|webm|mkv|mov|m3u8|mpd)$", RegexOption.IGNORE_CASE)
        return if (known.containsMatchIn(base)) base else base + extensionFor(item.mime)
    }

    private fun extensionFor(mime: String): String = when {
        mime.contains("webm") -> ".webm"
        mime.contains("matroska") -> ".mkv"
        mime.contains("quicktime") -> ".mov"
        else -> ".mp4"
    }

    fun labelFor(item: KoiDetectedMedia): String = when {
        item.isStream -> "Stream"
        item.mime.contains("webm") -> "WEBM"
        item.mime.contains("matroska") -> "MKV"
        item.mime.contains("quicktime") -> "MOV"
        else -> "MP4"
    }
}
