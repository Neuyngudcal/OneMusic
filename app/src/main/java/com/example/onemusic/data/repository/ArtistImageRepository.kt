package com.example.onemusic.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Artist Image Repository with offline SharedPreferences caching.
 * Fetches Ultra-High-Definition artist photos (1000x1000px) from Deezer Artist API,
 * with fallback to iTunes Artist/Album API.
 */
class ArtistImageRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("onemusic_artist_images_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _artistImagesFlow = MutableStateFlow<Map<String, String>>(loadCachedMap())
    val artistImagesFlow: StateFlow<Map<String, String>> = _artistImagesFlow.asStateFlow()

    private val ongoingFetches = mutableSetOf<String>()

    private fun loadCachedMap(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if (value is String && value.isNotBlank()) {
                result[key] = value
            }
        }
        return result
    }

    /**
     * Synchronously returns cached artist image URL or null if not yet available.
     */
    fun getCachedImageUrl(artistName: String): String? {
        val key = normalizeArtistKey(artistName)
        if (key.isBlank()) return null
        return _artistImagesFlow.value[key]
    }

    /**
     * Prefetches artist images in background if enabled and missing from cache.
     */
    fun prefetchArtists(artists: List<String>, isAutoDownloadEnabled: Boolean) {
        if (!isAutoDownloadEnabled) return

        scope.launch {
            val currentMap = _artistImagesFlow.value
            val missing = artists.map { it.trim() }
                .filter { it.isNotBlank() && it != "Nghệ sĩ chưa rõ" && it != "<unknown>" }
                .distinct()
                .filter { !currentMap.containsKey(normalizeArtistKey(it)) }

            for (artist in missing) {
                val key = normalizeArtistKey(artist)
                synchronized(ongoingFetches) {
                    if (ongoingFetches.contains(key)) return@synchronized
                    ongoingFetches.add(key)
                }

                try {
                    val imageUrl = fetchArtistImageOnline(artist)
                    if (!imageUrl.isNullOrBlank()) {
                        saveArtistImage(key, imageUrl)
                    }
                } catch (e: Exception) {
                    Log.d("ArtistImageRepository", "Error fetching artist image for $artist: ${e.message}")
                } finally {
                    synchronized(ongoingFetches) {
                        ongoingFetches.remove(key)
                    }
                }
                kotlinx.coroutines.delay(120) // Gentle throttling for network requests
            }
        }
    }

    /**
     * Fetches image for a single artist asynchronously and updates cache.
     */
    suspend fun fetchArtistImage(artistName: String, isAutoDownloadEnabled: Boolean = true): String? =
        withContext(Dispatchers.IO) {
            val key = normalizeArtistKey(artistName)
            if (key.isBlank()) return@withContext null

            val cached = _artistImagesFlow.value[key]
            if (cached != null) return@withContext cached

            if (!isAutoDownloadEnabled) return@withContext null

            try {
                val imageUrl = fetchArtistImageOnline(artistName)
                if (!imageUrl.isNullOrBlank()) {
                    saveArtistImage(key, imageUrl)
                    return@withContext imageUrl
                }
            } catch (e: Exception) {
                Log.d("ArtistImageRepository", "Failed to fetch image for $artistName: ${e.message}")
            }
            null
        }

    private fun saveArtistImage(key: String, url: String) {
        prefs.edit().putString(key, url).apply()
        val updated = _artistImagesFlow.value.toMutableMap()
        updated[key] = url
        _artistImagesFlow.value = updated
    }

    /**
     * Clears all cached artist images.
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        _artistImagesFlow.value = emptyMap()
    }

    /**
     * Online fetch combining Deezer API (1000x1000px) and iTunes API fallback.
     */
    private fun fetchArtistImageOnline(artistName: String): String? {
        val cleanName = cleanArtistQuery(artistName)
        if (cleanName.isBlank()) return null

        // 1. Primary: Deezer Artist API (1000x1000 Ultra HD)
        val deezerUrl = "https://api.deezer.com/search/artist?q=${URLEncoder.encode(cleanName, "UTF-8")}"
        val deezerResponse = makeHttpRequest(deezerUrl)
        if (!deezerResponse.isNullOrBlank()) {
            val extracted = extractDeezerImage(deezerResponse)
            if (!extracted.isNullOrBlank()) return extracted
        }

        // 2. Fallback: iTunes Artist Search API (High-Res 1000x1000)
        val itunesUrl = "https://itunes.apple.com/search?term=${URLEncoder.encode(cleanName, "UTF-8")}&entity=album&limit=1"
        val itunesResponse = makeHttpRequest(itunesUrl)
        if (!itunesResponse.isNullOrBlank()) {
            val extracted = extractItunesImage(itunesResponse)
            if (!extracted.isNullOrBlank()) return extracted
        }

        return null
    }

    private fun extractDeezerImage(jsonStr: String): String? {
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val dataArray = root["data"]?.jsonArray
            if (dataArray != null && dataArray.isNotEmpty()) {
                val firstArtist = dataArray[0].jsonObject
                // Prefer picture_xl (1000x1000), then picture_big (500x500), then picture_medium (250x250)
                firstArtist["picture_xl"]?.jsonPrimitive?.content
                    ?: firstArtist["picture_big"]?.jsonPrimitive?.content
                    ?: firstArtist["picture_medium"]?.jsonPrimitive?.content
                    ?: firstArtist["picture"]?.jsonPrimitive?.content
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractItunesImage(jsonStr: String): String? {
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val results = root["results"]?.jsonArray
            if (results != null && results.isNotEmpty()) {
                val firstItem = results[0].jsonObject
                val rawArtwork = firstItem["artworkUrl100"]?.jsonPrimitive?.content
                if (!rawArtwork.isNullOrBlank()) {
                    // Upgrade standard 100x100 artwork to 1000x1000 Ultra HD
                    return rawArtwork.replace("100x100bb.jpg", "1000x1000bb.jpg")
                        .replace("100x100bb.png", "1000x1000bb.png")
                }
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
                connectTimeout = 5000
                readTimeout = 6000
                setRequestProperty("User-Agent", "OneMusic-OneUI/1.0 (Android; Samsung Galaxy)")
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanArtistQuery(name: String): String {
        return name
            .replace(Regex("(?i)\\s*(ft\\.|feat\\.|featuring|prod\\.|x|&|,).*"), "")
            .replace(Regex("(?i)\\[.*?\\]|\\(.*?\\)"), "")
            .trim()
    }

    private fun normalizeArtistKey(artist: String): String {
        return artist.trim().lowercase()
    }

    companion object {
        @Volatile
        private var instance: ArtistImageRepository? = null

        fun getInstance(context: Context): ArtistImageRepository {
            return instance ?: synchronized(this) {
                instance ?: ArtistImageRepository(context).also { instance = it }
            }
        }
    }
}
