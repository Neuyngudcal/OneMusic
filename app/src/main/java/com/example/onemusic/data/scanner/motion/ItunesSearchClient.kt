package com.example.onemusic.data.scanner.motion

import android.util.Log
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.AppleMusicMotionFetcher.Companion.isGenericAlbum
import com.example.onemusic.data.scanner.AppleMusicMotionFetcher.Companion.isGenericArtist
import com.example.onemusic.data.scanner.motion.MotionHttpClient.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

/** Bước 1: tìm `collectionId` (album trên Apple Music) của một bài qua iTunes Search API. */
internal object ItunesSearchClient {
    private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
    private const val DURATION_TOLERANCE_MS = 8000L

    private val STRIP_PATTERNS = listOf(
        Regex("""\s*\(feat\.?\s*[^)]*\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[feat\.?\s*[^\]]*]""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\.?\s*[^)]*\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[Explicit]""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Explicit\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Remastered\s*\d*\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Remaster\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Deluxe[^)]*\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Live[^)]*\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*-\s*Single$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(Bonus Track Version\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*-\s*EP$""", RegexOption.IGNORE_CASE),
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun findCollectionId(track: Track): String? = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizeQuery(track.title)
        val cleanArtist = sanitizeQuery(track.artist)
        val cleanAlbum = sanitizeQuery(track.album)

        // Strategy 1: Title + Artist (Primary and most accurate)
        if (cleanTitle.isNotBlank()) {
            val q1 = if (cleanArtist.isNotBlank() && !isGenericArtist(cleanArtist)) "$cleanTitle $cleanArtist" else cleanTitle
            val match1 = queryItunes(q1, track)
            if (match1 != null) return@withContext match1
        }

        // Strategy 2: Album + Artist (only for valid non-generic albums and artists)
        if (cleanAlbum.isNotBlank() && !isGenericAlbum(cleanAlbum) && !isGenericArtist(cleanArtist) && !cleanAlbum.equals(cleanTitle, ignoreCase = true)) {
            val q2 = "$cleanAlbum $cleanArtist"
            val match2 = queryItunes(q2, track)
            if (match2 != null) return@withContext match2
        }

        // Broad artist-only search (Strategy 3) has been intentionally removed to prevent mismatching albums
        null
    }

    private suspend fun queryItunes(query: String, track: Track, limit: Int = 10): String? =
        withContext(Dispatchers.IO) {
            val encodedQuery = runCatching { URLEncoder.encode(query, "UTF-8") }.getOrElse { query }
            val urlString = "$ITUNES_SEARCH_URL?term=$encodedQuery&media=music&explicit=Yes&limit=$limit&country=us"
            Log.d(TAG, "🔎 Querying iTunes: '$query' -> $urlString")

            val responseJson = MotionHttpClient.get(urlString) ?: return@withContext null

            val results = runCatching {
                val root = json.parseToJsonElement(responseJson).jsonObject
                root["results"]?.jsonArray
            }.getOrNull() ?: return@withContext null

            Log.d(TAG, "📦 iTunes returned ${results.size} results for query '$query'")

            pickBestCollectionId(results, track) { Log.d(TAG, it) }
        }

    /**
     * Chấm điểm từng kết quả iTunes so với [track] (tên bài, nghệ sĩ, album, thời lượng) và chọn album
     * có điểm cao nhất ≥ 0.75. Hàm thuần (không gọi mạng) – mỗi ứng viên tốt hơn được báo qua [onCandidate].
     */
    fun pickBestCollectionId(results: JsonArray, track: Track, onCandidate: (String) -> Unit = {}): String? {
        var bestCollectionId: String? = null
        var bestScore = 0f

        val targetTitle = sanitizeQuery(track.title).lowercase()
        val targetArtist = sanitizeQuery(track.artist).lowercase()
        val targetAlbum = sanitizeQuery(track.album).lowercase()

        for (result in results) {
            val obj = result.jsonObject
            val itunesTitle = obj["trackName"]?.jsonPrimitive?.content ?: ""
            val itunesArtist = obj["artistName"]?.jsonPrimitive?.content ?: ""
            val itunesAlbum = obj["collectionName"]?.jsonPrimitive?.content ?: ""
            val collectionId = obj["collectionId"]?.jsonPrimitive?.content ?: continue
            val trackTimeMillis = runCatching {
                obj["trackTimeMillis"]?.jsonPrimitive?.content?.toLongOrNull()
            }.getOrNull() ?: 0L

            val cleanItunesTitle = sanitizeQuery(itunesTitle).lowercase()
            val cleanItunesArtist = sanitizeQuery(itunesArtist).lowercase()
            val cleanItunesAlbum = sanitizeQuery(itunesAlbum).lowercase()

            val titleSim = StringSimilarity.levenshteinSimilarity(targetTitle, cleanItunesTitle)
            val artistSim = StringSimilarity.levenshteinSimilarity(targetArtist, cleanItunesArtist)
            val albumSim = StringSimilarity.levenshteinSimilarity(targetAlbum, cleanItunesAlbum)

            val durationMatch = if (trackTimeMillis > 0 && track.durationMs > 0) {
                kotlin.math.abs(trackTimeMillis - track.durationMs) <= DURATION_TOLERANCE_MS
            } else false

            // Calculate matching score strictly (requires high confidence)
            var score = 0f
            if (titleSim >= 0.70f && (artistSim >= 0.65f || isGenericArtist(targetArtist))) {
                score = (titleSim * 0.50f) + (artistSim * 0.35f) + (if (durationMatch) 0.15f else 0f)
            } else if (!isGenericAlbum(targetAlbum) && !isGenericArtist(targetArtist) && albumSim >= 0.80f && artistSim >= 0.75f) {
                score = (albumSim * 0.50f) + (artistSim * 0.50f)
            }

            // Strict threshold: score must be at least 0.75 to prevent false matches
            if (score > bestScore && score >= 0.75f) {
                bestScore = score
                bestCollectionId = collectionId
                onCandidate("⭐ Candidate match: $collectionId ('$itunesTitle' in '$itunesAlbum' by '$itunesArtist', score=$score)")
            }
        }

        return bestCollectionId
    }

    /** Bỏ phần "(feat. …)", "[Explicit]", "(Remastered)", "- Single"… để tìm kiếm chính xác hơn. */
    fun sanitizeQuery(input: String): String {
        var result = input.trim()
        for (pattern in STRIP_PATTERNS) {
            result = pattern.replace(result, "")
        }
        return result.trim()
    }
}
