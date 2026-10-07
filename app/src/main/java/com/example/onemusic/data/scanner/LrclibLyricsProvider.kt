package com.example.onemusic.data.scanner

import android.content.Context
import com.example.onemusic.data.model.LyricLine
import com.example.onemusic.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Online Synced Lyrics Provider powered by LRCLIB (Open-Source Lyrics Database).
 * - Queries https://lrclib.net/api/get for exact matches.
 * - Falls back to https://lrclib.net/api/search if needed.
 * - Caches retrieved LRC files locally in cacheDir/lyrics/ for instant offline playback.
 */
object LrclibLyricsProvider {

    private const val BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "OneMusic-OneUI/1.0 (https://github.com/onemusic)"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Gets cached lyrics, local sidecar .lrc files, or fetches from LRCLIB online.
     */
    suspend fun getLyrics(context: Context, track: Track): List<LyricLine> = withContext(Dispatchers.IO) {
        // 1. Check local sidecar .lrc file in the same directory (e.g. Song.flac -> Song.lrc)
        val sidecarLyrics = loadSidecarLrc(context, track)
        if (sidecarLyrics.isNotEmpty()) {
            return@withContext sidecarLyrics
        }

        // 2. Check local file cache
        val cachedLyrics = loadCachedLyrics(context, track.id, track.durationMs)
        if (cachedLyrics.isNotEmpty()) {
            return@withContext cachedLyrics
        }

        // 3. Fetch from LRCLIB API
        try {
            val lrcContent = fetchFromLrclib(track)
            if (!lrcContent.isNullOrBlank()) {
                saveCachedLyrics(context, track.id, lrcContent)
                return@withContext LrcParser.parseLrc(lrcContent, track.durationMs)
            }
        } catch (_: Exception) {}

        emptyList()
    }

    private fun loadSidecarLrc(context: Context, track: Track): List<LyricLine> {
        return try {
            val audioUrl = track.audioUrl
            if (audioUrl.startsWith("file://") || audioUrl.startsWith("/")) {
                val filePath = if (audioUrl.startsWith("file://")) audioUrl.removePrefix("file://") else audioUrl
                val audioFile = File(filePath)
                val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() > 0) {
                    val content = lrcFile.readText(Charsets.UTF_8)
                    return LrcParser.parseLrc(content, track.durationMs)
                }
            }
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchFromLrclib(track: Track): String? {
        val cleanTitle = cleanTrackTitle(track.title)
        val cleanArtist = cleanArtistName(track.artist)
        val durationSec = (track.durationMs / 1000).toInt()

        // 1. Try exact get endpoint
        val getUrlString = buildString {
            append("$BASE_URL/get?")
            append("track_name=").append(urlEncode(cleanTitle))
            append("&artist_name=").append(urlEncode(cleanArtist))
            if (track.album.isNotBlank() && !track.album.contains("Thư mục", ignoreCase = true)) {
                append("&album_name=").append(urlEncode(track.album))
            }
            if (durationSec > 10) {
                append("&duration=").append(durationSec)
            }
        }

        val getResponse = makeHttpRequest(getUrlString)
        if (getResponse != null) {
            val lrc = extractLrcFromJson(getResponse)
            if (!lrc.isNullOrBlank()) return lrc
        }

        // 2. Fallback to search endpoint
        val searchUrlString = "$BASE_URL/search?q=${urlEncode("$cleanTitle $cleanArtist")}"
        val searchResponse = makeHttpRequest(searchUrlString)
        if (searchResponse != null) {
            return extractLrcFromSearchJson(searchResponse)
        }

        return null
    }

    private fun extractLrcFromJson(jsonStr: String): String? {
        return try {
            val element = json.parseToJsonElement(jsonStr).jsonObject
            val synced = element["syncedLyrics"]?.jsonPrimitive?.content
            if (!synced.isNullOrBlank()) return synced
            val plain = element["plainLyrics"]?.jsonPrimitive?.content
            if (!plain.isNullOrBlank()) return plain
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractLrcFromSearchJson(jsonStr: String): String? {
        return try {
            val array = json.parseToJsonElement(jsonStr).jsonArray
            for (item in array) {
                val obj = item.jsonObject
                val synced = obj["syncedLyrics"]?.jsonPrimitive?.content
                if (!synced.isNullOrBlank()) return synced
            }
            for (item in array) {
                val obj = item.jsonObject
                val plain = obj["plainLyrics"]?.jsonPrimitive?.content
                if (!plain.isNullOrBlank()) return plain
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun makeHttpRequest(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun loadCachedLyrics(context: Context, trackId: String, durationMs: Long = 0L): List<LyricLine> {
        return try {
            val lyricsDir = File(context.cacheDir, "lyrics")
            val lrcFile = File(lyricsDir, "${trackId}.lrc")
            if (lrcFile.exists() && lrcFile.length() > 0) {
                val content = lrcFile.readText()
                LrcParser.parseLrc(content, durationMs)
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCachedLyrics(context: Context, trackId: String, content: String) {
        try {
            val lyricsDir = File(context.cacheDir, "lyrics").apply { if (!exists()) mkdirs() }
            val lrcFile = File(lyricsDir, "${trackId}.lrc")
            lrcFile.writeText(content)
        } catch (_: Exception) {}
    }

    private fun cleanTrackTitle(title: String): String {
        return title.replace(Regex("""\s*[\(\[].*?[\)\]]"""), "") // Remove (Official Video), [Remastered]
            .replace(Regex("""\.mp3|\.flac|\.wav|\.m4a""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return if (artist.contains("chưa rõ", ignoreCase = true) || artist.contains("Unknown", ignoreCase = true)) ""
        else artist.split(',', '&', '/').firstOrNull()?.trim() ?: artist.trim()
    }

    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }
}
