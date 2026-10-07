package com.example.onemusic.data.scanner

import android.content.Context
import android.util.Log
import com.example.onemusic.data.local.MotionArtworkDao
import com.example.onemusic.data.local.OneMusicDatabase
import com.example.onemusic.data.model.MotionArtworkEntity
import com.example.onemusic.data.model.Track
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AppleMusicMotionFetcher: Fetches animated album artwork (editorialVideo)
 * using Apple Music's AMP API and downloads high-quality looping MP4 video to local storage.
 *
 * Flow:
 * 1. Multi-Strategy iTunes Search API (track + artist -> album + artist -> artist fallback)
 * 2. Apple Music AMP API (`https://amp-api.music.apple.com/v1/catalog/us/albums/{collectionId}?extend=editorialVideo`)
 *    with JWT Bearer authentication (dynamic token extraction with fallback)
 * 3. Master HLS m3u8 playlist parse -> select optimal AVC/H.264 resolution
 * 4. Variant HLS playlist parse -> extract full direct MP4 stream -> download single file to cache
 *
 * 100% Zero-crash defensive coding, Dispatchers.IO, zero `!!` operators.
 */
class AppleMusicMotionFetcher(private val context: Context) {

    companion object {
        private const val TAG = "MotionFetcher"
        private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
        private const val AMP_API_URL = "https://amp-api.music.apple.com/v1/catalog/us/albums"
        private const val APPLE_MUSIC_WEB_URL = "https://music.apple.com"
        private const val REQUEST_DELAY_MS = 1200L
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 25_000
        private const val NEGATIVE_CACHE_EXPIRY_DAYS = 30L
        private const val NEGATIVE_CACHE_EXPIRY_MS = NEGATIVE_CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
        private const val LEVENSHTEIN_THRESHOLD = 0.75f
        private const val DURATION_TOLERANCE_MS = 8000L

        private val GENERIC_ALBUM_NAMES = setOf(
            "single", "ep", "unknown", "unknown album", "<unknown>", "music", "download",
            "soundtrack", "ost", "thư viện thiết bị", "thư mục cục bộ", "album", "audio",
            "various artists", "various", "bài hát chưa đặt tên"
        )
        private val GENERIC_ARTIST_NAMES = setOf(
            "unknown", "unknown artist", "<unknown>", "nghệ sĩ chưa rõ", "various artists",
            "various", "va", "artist", "ca sĩ chưa rõ"
        )

        fun isGenericAlbum(name: String): Boolean {
            val clean = name.lowercase().trim()
            return clean.isBlank() || clean in GENERIC_ALBUM_NAMES || clean.startsWith("thư viện") || clean.startsWith("thư mục")
        }

        fun isGenericArtist(name: String): Boolean {
            val clean = name.lowercase().trim()
            return clean.isBlank() || clean in GENERIC_ARTIST_NAMES || clean.startsWith("nghệ sĩ")
        }

        private const val DEFAULT_JWT_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzg2NjMyOTI0LCJleHAiOjE3OTI2ODA5MjQsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.hBgj61sZf-y7bmuvT-joXAUAcf7TVJ51732xnH5vFkLHOmsQHxVqGMYUuI4h8c0-RX3fRY3moylhLW8fewFJyw"

        private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

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
    }

    private val dao: MotionArtworkDao = OneMusicDatabase.getInstance(context).motionArtworkDao()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.message}")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val prefs = context.getSharedPreferences("apple_motion_prefs", Context.MODE_PRIVATE)
    private var currentJwtToken: String = prefs.getString("jwt_token", DEFAULT_JWT_TOKEN) ?: DEFAULT_JWT_TOKEN
    private var batchJob: Job? = null

    // ========================================================================================
    // PUBLIC API
    // ========================================================================================

    /**
     * Get all currently downloaded motion artwork entities for startup preloading.
     */
    suspend fun getAllDownloadedEntities(): List<MotionArtworkEntity> = withContext(Dispatchers.IO) {
        dao.getAllDownloaded()
    }

    /**
     * Check if a track (or its album) has a locally downloaded motion artwork video.
     */
    suspend fun getLocalMotionVideoPath(track: Track): String? = withContext(Dispatchers.IO) {
        val cleanAlbum = track.album.trim()
        val cleanArtist = track.artist.trim()

        val entity = dao.getByTrackId(track.id)
            ?: (if (!isGenericArtist(cleanArtist) && !isGenericAlbum(cleanAlbum)) dao.getByArtistAndAlbum(cleanArtist, cleanAlbum) else null)
            ?: return@withContext null
        if (!entity.hasMotion || !entity.isDownloaded) return@withContext null

        val file = File(entity.localVideoPath)
        // Prioritize square 1:1 video if present, then check existing path or tall
        if (entity.appleAlbumId.isNotBlank() && file.parentFile != null) {
            val squareFile = File(file.parentFile, "${entity.appleAlbumId}.mp4")
            if (squareFile.exists() && squareFile.length() > 0) {
                return@withContext squareFile.absolutePath
            }
        }

        if (file.exists() && file.length() > 0) entity.localVideoPath else null
    }

    /**
     * Fetch motion artwork for a single track on-demand.
     */
    suspend fun fetchMotionArtworkIfNeeded(track: Track): MotionArtworkEntity? = withContext(Dispatchers.IO) {
        val cleanAlbum = track.album.trim()
        val cleanArtist = track.artist.trim()

        // 1. Check direct cache for this track, or its specific artist+album (if valid)
        val cached = dao.getByTrackId(track.id)
            ?: (if (!isGenericArtist(cleanArtist) && !isGenericAlbum(cleanAlbum)) dao.getByArtistAndAlbum(cleanArtist, cleanAlbum) else null)

        if (cached != null) {
            if (cached.hasMotion && cached.isDownloaded) {
                val f = File(cached.localVideoPath)
                if (f.exists() && f.length() > 0) {
                    if (cached.trackId != track.id) {
                        dao.upsert(cached.copy(trackId = track.id))
                    }
                    return@withContext cached
                }
            }
            if (cached.hasMotion && !cached.isDownloaded) {
                return@withContext downloadAndSave(cached.copy(trackId = track.id))
            }
            // Only respect negative cache if we actually confirmed an album has no motion art (appleAlbumId is present)
            val now = System.currentTimeMillis()
            if (!cached.hasMotion && cached.appleAlbumId.isNotBlank() && (now - cached.cachedAt < NEGATIVE_CACHE_EXPIRY_MS)) {
                return@withContext cached
            }
        }

        // 2. Perform fresh search, API lookup and download
        return@withContext searchAndFetch(track)
    }

    /**
     * Start background batch scan for all tracks.
     */
    fun startBackgroundMotionScan(tracks: List<Track>) {
        batchJob?.cancel()
        batchJob = scope.launch {
            _isFetching.value = true
            try {
                dao.clearExpiredNegativeCache(System.currentTimeMillis() - NEGATIVE_CACHE_EXPIRY_MS)

                for (track in tracks) {
                    val cached = dao.getByTrackId(track.id)
                    if (cached != null) {
                        if (cached.hasMotion) continue
                        val now = System.currentTimeMillis()
                        if (now - cached.cachedAt < NEGATIVE_CACHE_EXPIRY_MS) continue
                    }

                    runCatching {
                        searchAndFetch(track)
                    }.onFailure { e ->
                        Log.w(TAG, "Batch scan error for ${track.title}: ${e.message}")
                    }

                    delay(REQUEST_DELAY_MS)
                }
            } finally {
                _isFetching.value = false
            }
        }
    }

    fun cancelBackgroundScan() {
        batchJob?.cancel()
        _isFetching.value = false
    }

    /**
     * Completely clear all downloaded motion artwork files and database records.
     */
    suspend fun clearAllMotionArtworkCache() = withContext(Dispatchers.IO) {
        runCatching {
            val videoDir = File(context.cacheDir, "motion_artwork")
            if (videoDir.exists() && videoDir.isDirectory) {
                videoDir.listFiles()?.forEach { it.delete() }
            }
            dao.clearAll()
            Log.d(TAG, "🧹 Cleared all motion artwork cache (files and database)")
        }
    }

    /**
     * Remove motion artwork for a specific track, delete associated files, and store negative cache.
     */
    suspend fun removeMotionArtworkForTrack(track: Track) = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.getByTrackId(track.id)
            if (entity != null) {
                if (entity.localVideoPath.isNotBlank()) {
                    val f = File(entity.localVideoPath)
                    if (f.exists()) f.delete()
                }
                if (entity.appleAlbumId.isNotBlank()) {
                    val videoDir = File(context.cacheDir, "motion_artwork")
                    File(videoDir, "${entity.appleAlbumId}.mp4").takeIf { it.exists() }?.delete()
                    File(videoDir, "${entity.appleAlbumId}_tall.mp4").takeIf { it.exists() }?.delete()
                    dao.deleteByAlbumId(entity.appleAlbumId)
                }
                dao.deleteByTrackId(track.id)
            }
            // Mark negative cache so it will not auto-fetch again
            saveNegativeCache(track, entity?.appleAlbumId ?: "")
            Log.d(TAG, "🚫 Removed motion artwork for track: ${track.title} and saved negative cache")
        }
    }

    // ========================================================================================
    // CORE WORKFLOW
    // ========================================================================================

    private suspend fun searchAndFetch(track: Track): MotionArtworkEntity? {
        Log.d(TAG, "🔍 Starting motion search for: '${track.title}' by '${track.artist}' (Album: '${track.album}')")

        // Step 1: Search iTunes API to get collectionId
        val collectionId = searchItunesWithFallback(track)
        if (collectionId == null || collectionId.isBlank()) {
            Log.d(TAG, "❌ No Apple Music match found for: ${track.title}")
            saveNegativeCache(track, "")
            return null
        }

        Log.d(TAG, "🎯 Found Apple Music collectionId: $collectionId for '${track.title}'")

        // Step 2: Check if another track with this collectionId already has motion art cached
        val existingAlbum = dao.getByAlbumId(collectionId)
        if (existingAlbum != null) {
            Log.d(TAG, "📦 Found album cache hit for collectionId: $collectionId")
            val entity = existingAlbum.copy(
                trackId = track.id,
                cachedAt = System.currentTimeMillis()
            )
            dao.upsert(entity)
            if (entity.hasMotion && entity.isDownloaded && File(entity.localVideoPath).exists()) return entity
            if (entity.hasMotion && !entity.isDownloaded) return downloadAndSave(entity)
            if (!entity.hasMotion) return entity
        }

        // Step 3: Query Apple Music AMP API for editorialVideo
        val motionResult = fetchEditorialVideoFromAmpApi(collectionId)
        if (motionResult == null) {
            Log.d(TAG, "ℹ️ Album $collectionId has no editorialVideo motion artwork")
            saveNegativeCache(track, collectionId)
            return null
        }

        Log.d(TAG, "🎬 Found motion artwork m3u8: ${motionResult.preferredVideoUrl} (isTall=${motionResult.isTall})")

        // Step 4: Save metadata and return immediately for instant streaming (0s delay)
        val entity = MotionArtworkEntity(
            trackId = track.id,
            appleAlbumId = collectionId,
            albumName = track.album,
            artistName = track.artist,
            motionSquareUrl = motionResult.squareUrl,
            motionTallUrl = motionResult.tallUrl,
            hasMotion = true,
            isDownloaded = false,
            cachedAt = System.currentTimeMillis()
        )
        dao.upsert(entity)

        // Asynchronously download video to local disk in background without blocking video start
        scope.launch(Dispatchers.IO) {
            downloadAndSave(entity)
        }

        return entity
    }

    // ========================================================================================
    // STEP 1: Multi-Strategy iTunes Search
    // ========================================================================================

    private suspend fun searchItunesWithFallback(track: Track): String? = withContext(Dispatchers.IO) {
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

            val responseJson = httpGet(urlString) ?: return@withContext null

            val results = runCatching {
                val root = json.parseToJsonElement(responseJson).jsonObject
                root["results"]?.jsonArray
            }.getOrNull() ?: return@withContext null

            Log.d(TAG, "📦 iTunes returned ${results.size} results for query '$query'")

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

                val titleSim = levenshteinSimilarity(targetTitle, cleanItunesTitle)
                val artistSim = levenshteinSimilarity(targetArtist, cleanItunesArtist)
                val albumSim = levenshteinSimilarity(targetAlbum, cleanItunesAlbum)

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
                    Log.d(TAG, "⭐ Candidate match: $collectionId ('$itunesTitle' in '$itunesAlbum' by '$itunesArtist', score=$score)")
                }
            }

            bestCollectionId
        }

    // ========================================================================================
    // STEP 2: Apple Music AMP API (editorialVideo Extraction)
    // ========================================================================================

    data class EditorialVideoResult(
        val preferredVideoUrl: String,
        val tallUrl: String = "",
        val squareUrl: String = "",
        val isTall: Boolean = false
    )

    /**
     * Query Apple Music AMP API directly to extract editorialVideo motion artwork URL.
     * Prioritizes motionDetailTall / motionTallVideo3x4 (3:4 tall aspect ratio) for full-screen smartphone player.
     */
    private suspend fun fetchEditorialVideoFromAmpApi(collectionId: String): EditorialVideoResult? =
        withContext(Dispatchers.IO) {
            val apiUrl = "$AMP_API_URL/$collectionId?extend=editorialVideo,editorialArtwork"

            var responseJson = httpGetWithAuth(apiUrl, currentJwtToken)

            // If 401 Unauthorized, refresh the token and retry once
            if (responseJson == null) {
                Log.w(TAG, "AMP API request failed, attempting to refresh JWT token...")
                val refreshedToken = refreshJwtTokenFromWeb()
                if (refreshedToken != null && refreshedToken != currentJwtToken) {
                    currentJwtToken = refreshedToken
                    responseJson = httpGetWithAuth(apiUrl, currentJwtToken)
                }
            }

            if (responseJson == null) return@withContext null

            return@withContext runCatching {
                val root = json.parseToJsonElement(responseJson).jsonObject
                val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching null
                val attributes = data["attributes"]?.jsonObject ?: return@runCatching null
                val editorialVideo = attributes["editorialVideo"]?.jsonObject ?: return@runCatching null

                // 1. Square formats FIRST (motionDetailSquare / motionSquareVideo1x1 / motionArtistSquare1x1 for 1:1 square matching album artwork)
                val squareKeys = listOf("motionDetailSquare", "motionSquareVideo1x1", "motionArtistSquare1x1")
                var squareUrl: String? = null
                for (key in squareKeys) {
                    val v = editorialVideo[key]?.jsonObject?.get("video")?.jsonPrimitive?.content
                    if (!v.isNullOrBlank() && v.contains("mvod.itunes.apple.com")) {
                        squareUrl = v
                        break
                    }
                }

                // 2. Tall formats fallback (motionDetailTall / motionTallVideo3x4)
                val tallKeys = listOf("motionDetailTall", "motionTallVideo3x4")
                var tallUrl: String? = null
                for (key in tallKeys) {
                    val v = editorialVideo[key]?.jsonObject?.get("video")?.jsonPrimitive?.content
                    if (!v.isNullOrBlank() && v.contains("mvod.itunes.apple.com")) {
                        tallUrl = v
                        break
                    }
                }

                val preferred = squareUrl ?: tallUrl ?: return@runCatching null

                EditorialVideoResult(
                    preferredVideoUrl = preferred,
                    tallUrl = tallUrl ?: "",
                    squareUrl = squareUrl ?: "",
                    isTall = tallUrl != null && squareUrl == null
                )
            }.getOrNull()
        }

    /**
     * Dynamically extracts the latest JWT developer token from Apple Music Web assets.
     */
    private suspend fun refreshJwtTokenFromWeb(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val browseHtml = httpGet("$APPLE_MUSIC_WEB_URL/us/browse") ?: return@runCatching null
            val jsPathMatch = Regex("""src="(/assets/index~[a-zA-Z0-9_-]+\.js)"""").find(browseHtml)
            val jsPath = jsPathMatch?.groupValues?.get(1) ?: return@runCatching null

            val jsContent = httpGet("$APPLE_MUSIC_WEB_URL$jsPath") ?: return@runCatching null
            val tokenMatch = Regex("""(eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,})""").find(jsContent)
            val token = tokenMatch?.groupValues?.get(1)
            if (!token.isNullOrBlank()) {
                Log.d(TAG, "Successfully extracted fresh JWT token from Apple Music web")
                prefs.edit().putString("jwt_token", token).apply()
                token
            } else null
        }.getOrNull()
    }

    // ========================================================================================
    // STEP 3 & 4: Download Video from HLS Master Playlist
    // ========================================================================================

    private suspend fun downloadAndSave(entity: MotionArtworkEntity): MotionArtworkEntity? =
        withContext(Dispatchers.IO) {
            val hasSquare = entity.motionSquareUrl.isNotBlank()
            val m3u8Url = if (hasSquare) entity.motionSquareUrl else entity.motionTallUrl
            if (m3u8Url.isBlank()) return@withContext entity

            val videoDir = File(context.cacheDir, "motion_artwork").apply {
                if (!exists()) mkdirs()
            }
            val videoFile = File(videoDir, if (hasSquare) "${entity.appleAlbumId}.mp4" else "${entity.appleAlbumId}_tall.mp4")

            // Skip if already downloaded and valid
            if (videoFile.exists() && videoFile.length() > 1024) {
                val updated = entity.copy(
                    localVideoPath = videoFile.absolutePath,
                    isDownloaded = true
                )
                dao.upsert(updated)
                return@withContext updated
            }

            Log.d(TAG, "📥 Parsing master playlist: $m3u8Url")

            // 1. Parse master m3u8 to find optimal variant
            val variantM3u8Url = parseMasterPlaylistForBestVariant(m3u8Url) ?: run {
                Log.w(TAG, "Could not find valid variant in master m3u8")
                return@withContext entity
            }

            Log.d(TAG, "📥 Selected variant playlist: $variantM3u8Url")

            // 2. Parse variant m3u8 to extract direct MP4 URL or segments
            val directMp4Url = extractDirectMp4UrlFromVariant(variantM3u8Url)

            val success = if (directMp4Url != null) {
                Log.d(TAG, "📥 Downloading direct MP4 from: $directMp4Url")
                downloadFile(directMp4Url, videoFile)
            } else {
                Log.d(TAG, "📥 Downloading segmented HLS stream from: $variantM3u8Url")
                downloadHlsVariant(variantM3u8Url, videoFile)
            }

            if (success && videoFile.exists() && videoFile.length() > 0) {
                if (hasSquare) {
                    val oldTallFile = File(videoDir, "${entity.appleAlbumId}_tall.mp4")
                    if (oldTallFile.exists()) {
                        runCatching { oldTallFile.delete() }
                    }
                }
                val updated = entity.copy(
                    localVideoPath = videoFile.absolutePath,
                    isDownloaded = true
                )
                dao.upsert(updated)
                Log.d(TAG, "🎉 Motion artwork downloaded! Size: ${videoFile.length() / 1024} KB at: ${videoFile.absolutePath}")
                return@withContext updated
            } else {
                Log.w(TAG, "Failed to download motion artwork video file")
                return@withContext entity
            }
        }

    /**
     * Inspects a variant m3u8 playlist to see if it uses EXT-X-MAP pointing to a single .mp4 file.
     */
    private suspend fun extractDirectMp4UrlFromVariant(variantM3u8Url: String): String? =
        withContext(Dispatchers.IO) {
            val content = httpGet(variantM3u8Url) ?: return@withContext null
            val mapMatch = Regex("""#EXT-X-MAP:URI="([^"]+\.mp4)"""").find(content)
            val mp4RelativeOrAbsolute = mapMatch?.groupValues?.get(1) ?: return@withContext null
            resolveUrl(variantM3u8Url, mp4RelativeOrAbsolute)
        }

    /**
     * Parse HLS master playlist and select best AVC/H.264 variant (up to 1080p).
     */
    private suspend fun parseMasterPlaylistForBestVariant(m3u8Url: String): String? =
        withContext(Dispatchers.IO) {
            val content = httpGet(m3u8Url) ?: return@withContext null
            val lines = content.lines()

            data class Variant(val bandwidth: Long, val resolution: Int, val codec: String, val url: String)

            val variants = mutableListOf<Variant>()
            var currentBandwidth = 0L
            var currentResolution = 0
            var currentCodec = ""

            for (line in lines) {
                if (line.startsWith("#EXT-X-STREAM-INF:")) {
                    val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                    currentBandwidth = bwMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                    val resMatch = Regex("""RESOLUTION=(\d+)x(\d+)""").find(line)
                    currentResolution = resMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0

                    val codecMatch = Regex("""CODECS="([^"]+)"""").find(line)
                    currentCodec = codecMatch?.groupValues?.get(1) ?: ""
                } else if (!line.startsWith("#") && line.isNotBlank() && currentBandwidth > 0) {
                    val absoluteUrl = resolveUrl(m3u8Url, line.trim())
                    variants.add(Variant(currentBandwidth, currentResolution, currentCodec, absoluteUrl))
                    currentBandwidth = 0L
                    currentResolution = 0
                    currentCodec = ""
                }
            }

            if (variants.isEmpty()) return@withContext null

            // Prioritize highest quality 1080p AVC/H.264 variant for crystal-sharp display
            val avcVariants = variants.filter { it.codec.contains("avc1", ignoreCase = true) }
            val pool = if (avcVariants.isNotEmpty()) avcVariants else variants

            // Pick 1080p (or highest available resolution <= 1080p)
            val highestQualityVariant = pool.filter { it.resolution <= 1080 }.maxByOrNull { it.resolution }
                ?: pool.maxByOrNull { it.resolution }
                ?: pool.firstOrNull()

            highestQualityVariant?.url
        }

    private suspend fun downloadFile(url: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlObj = URL(url)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS * 2
                conn.setRequestProperty("User-Agent", USER_AGENT)

                try {
                    if (conn.responseCode != 200 && conn.responseCode != 206) return@runCatching false
                    val buffer = ByteArray(128 * 1024)
                    java.io.BufferedInputStream(conn.inputStream, 128 * 1024).use { bis ->
                        java.io.BufferedOutputStream(FileOutputStream(outputFile), 128 * 1024).use { bos ->
                            var bytesRead: Int
                            while (bis.read(buffer).also { bytesRead = it } != -1) {
                                bos.write(buffer, 0, bytesRead)
                            }
                            bos.flush()
                        }
                    }
                    outputFile.length() > 0
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { false }
        }

    private suspend fun downloadHlsVariant(variantM3u8Url: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val content = httpGet(variantM3u8Url) ?: return@withContext false
            val lines = content.lines()

            // 1. Check for fMP4 initialization segment (#EXT-X-MAP:URI="init.mp4")
            val initMapLine = lines.firstOrNull { it.startsWith("#EXT-X-MAP:") }
            val initUri = initMapLine?.let { Regex("""URI="([^"]+)"""").find(it)?.groupValues?.get(1) }
            val initUrl = initUri?.let { resolveUrl(variantM3u8Url, it.trim()) }

            val segmentUrls = lines.filter { !it.startsWith("#") && it.isNotBlank() }
                .map { resolveUrl(variantM3u8Url, it.trim()) }

            if (segmentUrls.isEmpty()) return@withContext false

            runCatching {
                coroutineScope {
                    // Download init segment and all video chunks concurrently in parallel
                    val initDeferred = initUrl?.let { url ->
                        async(Dispatchers.IO) { httpGetBytes(url) }
                    }

                    val segmentsDeferred = segmentUrls.mapIndexed { index, segUrl ->
                        async(Dispatchers.IO) {
                            Pair(index, httpGetBytes(segUrl))
                        }
                    }

                    val initBytes = initDeferred?.await()
                    val downloadedSegments = segmentsDeferred.awaitAll()

                    FileOutputStream(outputFile).use { fos ->
                        // Write fMP4 header (ftyp + moov) first so ExoPlayer can decode the file from disk
                        if (initBytes != null && initBytes.isNotEmpty()) {
                            fos.write(initBytes)
                        }
                        for ((_, segData) in downloadedSegments.sortedBy { it.first }) {
                            if (segData != null && segData.isNotEmpty()) {
                                fos.write(segData)
                            }
                        }
                    }
                }
                outputFile.length() > 0
            }.getOrElse { false }
        }

    // ========================================================================================
    // HELPER: HTTP & Data
    // ========================================================================================

    private suspend fun httpGetWithAuth(urlString: String, bearerToken: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Authorization", "Bearer $bearerToken")
                conn.setRequestProperty("Origin", "https://music.apple.com")
                conn.setRequestProperty("Accept", "application/json")

                try {
                    if (conn.responseCode != 200) {
                        Log.w(TAG, "HTTP ${conn.responseCode} for $urlString")
                        return@runCatching null
                    }
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

    private suspend fun httpGet(urlString: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            try {
                if (conn.responseCode != 200) {
                    Log.w(TAG, "HTTP ${conn.responseCode} for $urlString")
                    return@runCatching null
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private suspend fun httpGetBytes(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS * 2
            conn.setRequestProperty("User-Agent", USER_AGENT)

            try {
                if (conn.responseCode != 200) return@runCatching null
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private suspend fun saveNegativeCache(track: Track, albumId: String) {
        val entity = MotionArtworkEntity(
            trackId = track.id,
            appleAlbumId = albumId,
            albumName = track.album,
            artistName = track.artist,
            hasMotion = false,
            isDownloaded = false,
            cachedAt = System.currentTimeMillis()
        )
        dao.upsert(entity)
    }

    private fun sanitizeQuery(input: String): String {
        var result = input.trim()
        for (pattern in STRIP_PATTERNS) {
            result = pattern.replace(result, "")
        }
        return result.trim()
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        val lastSlash = baseUrl.lastIndexOf('/')
        return if (lastSlash > 0) {
            baseUrl.substring(0, lastSlash + 1) + relativeUrl
        } else {
            relativeUrl
        }
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}
