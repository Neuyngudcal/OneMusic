package com.example.onemusic.data.scanner

import android.content.Context
import android.util.Log
import com.example.onemusic.data.local.MotionArtworkDao
import com.example.onemusic.data.local.OneMusicDatabase
import com.example.onemusic.data.model.MotionArtworkEntity
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.motion.AppleMusicAmpClient
import com.example.onemusic.data.scanner.motion.HlsVideoDownloader
import com.example.onemusic.data.scanner.motion.ItunesSearchClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
        private const val REQUEST_DELAY_MS = 1200L
        private const val NEGATIVE_CACHE_EXPIRY_DAYS = 30L
        private const val NEGATIVE_CACHE_EXPIRY_MS = NEGATIVE_CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

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
    }

    private val dao: MotionArtworkDao = OneMusicDatabase.getInstance(context).motionArtworkDao()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.message}")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val ampClient = AppleMusicAmpClient(context.getSharedPreferences("apple_motion_prefs", Context.MODE_PRIVATE))
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
        val collectionId = ItunesSearchClient.findCollectionId(track)
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
        val motionResult = ampClient.fetchEditorialVideo(collectionId)
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
            val variantM3u8Url = HlsVideoDownloader.parseMasterPlaylistForBestVariant(m3u8Url) ?: run {
                Log.w(TAG, "Could not find valid variant in master m3u8")
                return@withContext entity
            }

            Log.d(TAG, "📥 Selected variant playlist: $variantM3u8Url")

            // 2. Parse variant m3u8 to extract direct MP4 URL or segments
            val directMp4Url = HlsVideoDownloader.extractDirectMp4UrlFromVariant(variantM3u8Url)

            val success = if (directMp4Url != null) {
                Log.d(TAG, "📥 Downloading direct MP4 from: $directMp4Url")
                HlsVideoDownloader.downloadFile(directMp4Url, videoFile)
            } else {
                Log.d(TAG, "📥 Downloading segmented HLS stream from: $variantM3u8Url")
                HlsVideoDownloader.downloadHlsVariant(variantM3u8Url, videoFile)
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

    // ========================================================================================
    // HELPER: Negative cache
    // ========================================================================================

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
}
