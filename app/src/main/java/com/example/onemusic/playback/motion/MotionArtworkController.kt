package com.example.onemusic.playback.motion

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.data.scanner.AppleMusicMotionFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Bìa động (Motion Artwork, video bìa kiểu Apple Music) của Now Playing.
 * Sở hữu: [player] (ExoPlayer riêng chỉ giải mã video, lặp vô hạn), [motionVideoPath],
 * bộ nhớ đệm RAM `trackId` / `nghệ sĩ_album` → đường dẫn video, và job tải bìa động.
 * Chỉ cần biết bài đang phát (qua [currentTrackId]) để bỏ kết quả tải về muộn của bài cũ.
 */
internal class MotionArtworkController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val musicRepository: MusicRepository,
    private val settingsPreferences: SettingsPreferences,
    private val currentTrackId: () -> String?,
) {
    // Motion Artwork State: Local video path for animated album art on Now Playing
    private val _motionVideoPath = MutableStateFlow<String?>(null)
    val motionVideoPath: StateFlow<String?> = _motionVideoPath.asStateFlow()
    private var motionJob: Job? = null

    // Dedicated Pre-warmed Motion Artwork Player instance (Single reusable instance for 0ms instant playback)
    var player: ExoPlayer? = null
        private set

    // Chỉ cho phép giải mã video bìa động khi Now Playing đang hiển thị (tránh hao pin khi chạy nền)
    private var isMotionPlaybackAllowed = false

    private var currentLoadedMotionPath: String? = null
    private val inMemoryMotionCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun setPlaybackAllowed(allowed: Boolean) {
        isMotionPlaybackAllowed = allowed
        player?.playWhenReady = allowed
    }

    fun preloadCacheFromDb() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val downloaded = musicRepository.getAllDownloadedMotionArtworks()
                for (entity in downloaded) {
                    val file = File(entity.localVideoPath)
                    if (file.exists() && file.length() > 0) {
                        if (entity.trackId.isNotBlank()) {
                            inMemoryMotionCache[entity.trackId] = entity.localVideoPath
                        }
                        if (entity.artistName.isNotBlank() && entity.albumName.isNotBlank() &&
                            !AppleMusicMotionFetcher.isGenericArtist(entity.artistName) &&
                            !AppleMusicMotionFetcher.isGenericAlbum(entity.albumName)
                        ) {
                            val key = "${entity.artistName.lowercase().trim()}_${entity.albumName.lowercase().trim()}"
                            inMemoryMotionCache[key] = entity.localVideoPath
                        }
                    }
                }
                android.util.Log.d("MotionArt", "🚀 Preloaded ${downloaded.size} motion artworks into RAM cache (0ms instant access)")
            }
        }
    }

    fun setupPlayer() {
        runCatching {
            val motionLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 30_000,
                    /* bufferForPlaybackMs = */ 0,
                    /* bufferForPlaybackAfterRebufferMs = */ 0
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioRenderers(
                    context: Context,
                    extensionRendererMode: Int,
                    mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                    enableDecoderFallback: Boolean,
                    audioSink: androidx.media3.exoplayer.audio.AudioSink,
                    eventHandler: android.os.Handler,
                    eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                    out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
                ) {
                    // Do not build audio renderers for motion artwork - pure video decoding for 0ms gapless loop
                }
            }

            player = ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(motionLoadControl)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                    playWhenReady = isMotionPlaybackAllowed
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                seekTo(0L)
                                playWhenReady = isMotionPlaybackAllowed
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            android.util.Log.e("MotionArt", "Motion player error: ${error.message}, recovering...")
                            val badPath = currentLoadedMotionPath
                            if (badPath != null && !badPath.startsWith("http")) {
                                runCatching { File(badPath).delete() }
                                inMemoryMotionCache.remove(badPath)
                            }
                        }
                    })
                }
        }
    }

    private fun updatePlayerMedia(pathOrUrl: String) {
        if (currentLoadedMotionPath == pathOrUrl && player?.playbackState != Player.STATE_IDLE) {
            return
        }
        currentLoadedMotionPath = pathOrUrl
        val isNetworkStream = pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")
        val uri = if (isNetworkStream) {
            android.net.Uri.parse(pathOrUrl)
        } else {
            val file = File(pathOrUrl)
            if (file.exists() && file.length() > 0) android.net.Uri.fromFile(file) else return
        }

        val item = if (pathOrUrl.contains(".m3u8")) {
            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        } else {
            MediaItem.fromUri(uri)
        }

        player?.setMediaItem(item)
        player?.prepare()
        player?.playWhenReady = isMotionPlaybackAllowed
    }

    /** Ẩn bìa động: xóa đường dẫn đang hiển thị và dừng/xóa media của player video. */
    private fun clearDisplayedMotion() {
        _motionVideoPath.value = null
        player?.stop()
        player?.clearMediaItems()
        currentLoadedMotionPath = null
    }

    /** Cài đặt thay đổi: tắt bìa động thì ẩn ngay; bật lại mà chưa có video thì tải cho [currentTrack]. */
    suspend fun onSettingsChanged(isMotionArtworkEnabled: Boolean, currentTrack: Track?) {
        if (!isMotionArtworkEnabled) {
            withContext(Dispatchers.Main) {
                clearDisplayedMotion()
            }
        } else if (_motionVideoPath.value == null) {
            currentTrack?.let { current ->
                fetchForTrack(current)
            }
        }
    }

    /** Predictive Motion Artwork Preload: download next & prev song's video before it plays. Chạy trên luồng IO. */
    suspend fun prefetchNeighbors(nextTrack: Track?, prevTrack: Track?) {
        if (settingsPreferences.getSettings().isMotionArtworkEnabled) {
            nextTrack?.let { next ->
                val cached = inMemoryMotionCache[next.id] ?: musicRepository.getLocalMotionVideoPath(next)
                if (cached != null) {
                    inMemoryMotionCache[next.id] = cached
                } else {
                    val result = musicRepository.getMotionArtwork(next)
                    if (result != null && result.hasMotion && result.isDownloaded && result.localVideoPath.isNotBlank()) {
                        inMemoryMotionCache[next.id] = result.localVideoPath
                    }
                }
            }
            if (prevTrack?.id != nextTrack?.id) {
                prevTrack?.let { prev ->
                    val cached = inMemoryMotionCache[prev.id] ?: musicRepository.getLocalMotionVideoPath(prev)
                    if (cached != null) {
                        inMemoryMotionCache[prev.id] = cached
                    }
                }
            }
        }
    }

    /**
     * Fetch animated/motion album artwork for a track.
     * Checks fast in-memory cache first (0ms), then local disk cache, then Apple Music.
     * Updates motionVideoPath StateFlow for UI to observe.
     */
    fun fetchForTrack(track: Track) {
        motionJob?.cancel()

        // If Motion Artwork is disabled by user in Settings, clear and exit immediately
        if (!settingsPreferences.getSettings().isMotionArtworkEnabled) {
            clearDisplayedMotion()
            return
        }

        // 1. Kiểm tra In-Memory Cache tức thì (0ms) bằng track ID hoặc Artist+Album (không bao giờ dùng albumName trần)
        val cleanAlbum = track.album.lowercase().trim()
        val cleanArtist = track.artist.lowercase().trim()
        val isGeneric = AppleMusicMotionFetcher.isGenericAlbum(cleanAlbum) ||
                AppleMusicMotionFetcher.isGenericArtist(cleanArtist)
        val artistAlbumKey = if (!isGeneric) "${cleanArtist}_${cleanAlbum}" else null

        val memCached = inMemoryMotionCache[track.id]
            ?: (if (artistAlbumKey != null) inMemoryMotionCache[artistAlbumKey] else null)

        if (memCached != null && File(memCached).exists() && File(memCached).length() > 0) {
            _motionVideoPath.value = memCached
            updatePlayerMedia(memCached)
            android.util.Log.d("MotionArt", "⚡ Instant in-memory cache hit (0ms): $memCached")
            return
        }

        // 2. Nếu chưa có trong RAM, RESET NGAY _motionVideoPath để không bao giờ bị kẹt ảnh của bài hát trước
        clearDisplayedMotion()

        android.util.Log.d("MotionArt", "🎬 Fetching motion art for: ${track.title} - ${track.artist}")
        motionJob = scope.launch(Dispatchers.IO) {
            val cachedPath = musicRepository.getLocalMotionVideoPath(track)
            if (cachedPath != null) {
                inMemoryMotionCache[track.id] = cachedPath
                if (artistAlbumKey != null) inMemoryMotionCache[artistAlbumKey] = cachedPath
                android.util.Log.d("MotionArt", "✅ Disk cache hit! Video at: $cachedPath")
                withContext(Dispatchers.Main) {
                    if (currentTrackId() == track.id) {
                        _motionVideoPath.value = cachedPath
                        updatePlayerMedia(cachedPath)
                    }
                }
                return@launch
            }

            android.util.Log.d("MotionArt", "🔍 No cache, querying Apple Music...")

            // 3. Nếu chưa có trên đĩa, truy vấn Apple Music để phát luồng HLS tức thì và tải ngầm vào bộ nhớ
            val result = runCatching {
                musicRepository.getMotionArtwork(track)
            }.onFailure { e ->
                android.util.Log.e("MotionArt", "❌ Fetch error: ${e.message}")
            }.getOrNull()

            if (result != null && result.hasMotion) {
                val streamOrLocal = if (result.isDownloaded && result.localVideoPath.isNotBlank() && File(result.localVideoPath).exists()) {
                    result.localVideoPath
                } else if (result.motionSquareUrl.isNotBlank()) {
                    result.motionSquareUrl
                } else null

                if (streamOrLocal != null) {
                    inMemoryMotionCache[track.id] = streamOrLocal
                    if (artistAlbumKey != null) inMemoryMotionCache[artistAlbumKey] = streamOrLocal
                    android.util.Log.d("MotionArt", "⚡ Instant Motion stream/file ready: $streamOrLocal")
                    withContext(Dispatchers.Main) {
                        if (currentTrackId() == track.id) {
                            _motionVideoPath.value = streamOrLocal
                            updatePlayerMedia(streamOrLocal)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (currentTrackId() == track.id) {
                        clearDisplayedMotion()
                    }
                }
            }
        }
    }

    /** Remove motion artwork for [track] (bài đang phát) and update UI immediately. */
    fun removeForTrack(track: Track) {
        scope.launch(Dispatchers.IO) {
            musicRepository.removeMotionArtworkForTrack(track)
            inMemoryMotionCache.remove(track.id)
            val cleanAlbum = track.album.lowercase().trim()
            val cleanArtist = track.artist.lowercase().trim()
            inMemoryMotionCache.remove("${cleanArtist}_${cleanAlbum}")
            withContext(Dispatchers.Main) {
                clearDisplayedMotion()
            }
        }
    }

    /** Completely wipe all motion artwork cache from RAM and disk. */
    fun clearCache() {
        scope.launch(Dispatchers.IO) {
            inMemoryMotionCache.clear()
            musicRepository.clearAllMotionArtworkCache()
            withContext(Dispatchers.Main) {
                clearDisplayedMotion()
            }
        }
    }

    fun cancelPendingFetch() {
        motionJob?.cancel()
    }

    fun releasePlayer() {
        try { player?.release() } catch (_: Exception) {}
        player = null
    }
}
