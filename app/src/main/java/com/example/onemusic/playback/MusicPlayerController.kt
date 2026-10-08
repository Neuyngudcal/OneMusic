package com.example.onemusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import java.io.File
import androidx.media3.session.MediaSession
import com.example.onemusic.MainActivity
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.LrclibLyricsProvider
import com.example.onemusic.playback.service.OneMusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MusicPlayerController(
    private val context: Context,
    val audioEffectManager: AudioEffectManager = AudioEffectManager(SettingsPreferences.getInstance(context))
) {
    companion object {
        @Volatile
        private var instance: MusicPlayerController? = null

        fun getInstance(
            context: Context,
            audioEffectManager: AudioEffectManager = AudioEffectManager(SettingsPreferences.getInstance(context))
        ): MusicPlayerController {
            val current = instance
            if (current != null && current.isAlive()) {
                return current
            }
            return synchronized(this) {
                val existing = instance
                if (existing != null && existing.isAlive()) {
                    existing
                } else {
                    MusicPlayerController(context.applicationContext, audioEffectManager).also {
                        instance = it
                    }
                }
            }
        }
    }

    fun isAlive(): Boolean = exoPlayer != null

    val settingsPreferences = SettingsPreferences(context)

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("MusicPlayerController", "Uncaught coroutine exception: ${throwable.message}")
    }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + coroutineExceptionHandler)
    private var lyricsJob: Job? = null
    private var userVolume: Float = 1.0f
    private var consecutivePlaybackErrors: Int = 0
    private var isReorderingQueue: Boolean = false

    // Shuffle kiểu Apple Music: state.queue luôn là THỨ TỰ PHÁT THẬT (đã xáo), ExoPlayer không bật shuffleModeEnabled.
    // originalQueue giữ thứ tự gốc để khôi phục khi tắt shuffle; khác null khi và chỉ khi đang shuffle.
    private var originalQueue: MutableList<Track>? = null

    // 1 & 2. Audiophile AudioSink with dynamic ReplayGain & Headroom Soft-Knee Limiter
    val headroomLimiter = HeadroomLimiterAudioProcessor(defaultGainDb = 0.0f)

    private val musicRepository = com.example.onemusic.data.repository.MusicRepository.getInstance(context)

    val audioOutputManager = AudioOutputManager(context) { audioDeviceInfo ->
        exoPlayer?.setPreferredAudioDevice(audioDeviceInfo)
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Vị trí phát tách ra luồng riêng: cập nhật mỗi 40ms mà không làm vẽ lại mọi màn hình đọc playbackState.
    // PlaybackState.currentPositionMs chỉ còn được đặt ở các sự kiện rời rạc (đổi bài, tua...), KHÔNG cập nhật liên tục.
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val volumeFader = VolumeFader(scope, player = { exoPlayer }, userVolume = { userVolume })
    private val positionTracker = PositionTracker(
        scope = scope,
        player = { exoPlayer },
        playbackState = _playbackState,
        positionMs = _positionMs,
        settingsPreferences = settingsPreferences,
        isFading = { volumeFader.isFading },
        userVolume = { userVolume }
    )
    private val sleepTimer = SleepTimer(scope, _playbackState) {
        smoothFadeOut(durationMs = 2000L) {
            exoPlayer?.pause()
            exoPlayer?.volume = userVolume
            _playbackState.value = _playbackState.value.copy(
                sleepTimerMinutes = null,
                sleepTimerRemainingSeconds = null,
                isPlaying = false
            )
        }
    }

    // Motion Artwork State: Local video path for animated album art on Now Playing
    private val _motionVideoPath = MutableStateFlow<String?>(null)
    val motionVideoPath: StateFlow<String?> = _motionVideoPath.asStateFlow()
    private var motionJob: Job? = null

    // Dedicated Pre-warmed Motion Artwork Player instance (Single reusable instance for 0ms instant playback)
    var motionExoPlayer: androidx.media3.exoplayer.ExoPlayer? = null
        private set

    // Chỉ cho phép giải mã video bìa động khi Now Playing đang hiển thị (tránh hao pin khi chạy nền)
    private var isMotionPlaybackAllowed = false

    fun setMotionPlaybackAllowed(allowed: Boolean) {
        isMotionPlaybackAllowed = allowed
        motionExoPlayer?.playWhenReady = allowed
    }

    init {
        setupPlayer()
        setupMotionPlayer()
        preloadMotionCacheFromDb()
        setupHeadsetCallbacks()
        setupSettingsObserver()
        setupWidgetObserver()
    }

    private fun preloadMotionCacheFromDb() {
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
                            !com.example.onemusic.data.scanner.AppleMusicMotionFetcher.isGenericArtist(entity.artistName) &&
                            !com.example.onemusic.data.scanner.AppleMusicMotionFetcher.isGenericAlbum(entity.albumName)
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

    private fun setupMotionPlayer() {
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

            motionExoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(motionLoadControl)
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                    volume = 0f
                    playWhenReady = isMotionPlaybackAllowed
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                        .build()
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == androidx.media3.common.Player.STATE_ENDED) {
                                seekTo(0L)
                                playWhenReady = isMotionPlaybackAllowed
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
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

    private var currentLoadedMotionPath: String? = null

    private fun updateMotionPlayerMedia(pathOrUrl: String) {
        if (currentLoadedMotionPath == pathOrUrl && motionExoPlayer?.playbackState != androidx.media3.common.Player.STATE_IDLE) {
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
            androidx.media3.common.MediaItem.Builder()
                .setUri(uri)
                .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                .build()
        } else {
            androidx.media3.common.MediaItem.fromUri(uri)
        }

        motionExoPlayer?.setMediaItem(item)
        motionExoPlayer?.prepare()
        motionExoPlayer?.playWhenReady = isMotionPlaybackAllowed
    }

    private fun setupWidgetObserver() {
        scope.launch {
            _playbackState.collect { state ->
                com.example.onemusic.widget.OneMusicWidgetUpdater.updateAllWidgets(context, state)
            }
        }
    }

    private fun setupHeadsetCallbacks() {
        audioOutputManager.onHeadsetConnected = {
            val settings = settingsPreferences.getSettings()
            if (settings.resumeOnHeadsetConnect && _playbackState.value.currentTrack != null && !_playbackState.value.isPlaying) {
                resume()
            }
        }
        audioOutputManager.onHeadsetDisconnected = {
            val settings = settingsPreferences.getSettings()
            if (settings.pauseOnHeadsetDisconnect && _playbackState.value.isPlaying) {
                pause()
            }
        }
    }

    fun resume() {
        val player = exoPlayer ?: return
        if (!player.isPlaying) {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED || player.mediaItemCount == 0) {
                val current = _playbackState.value.currentTrack
                if (current != null) {
                    playTrack(current, autoPlay = true)
                } else if (_playbackState.value.queue.isNotEmpty()) {
                    setQueue(_playbackState.value.queue, startIndex = 0, autoPlay = true)
                }
            } else {
                player.volume = userVolume
                smoothFadeIn()
            }
        }
    }

    private fun setupSettingsObserver() {
        scope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                updateReplayGainForTrack(_playbackState.value.currentTrack)
                if (!settings.isMotionArtworkEnabled) {
                    withContext(Dispatchers.Main) {
                        _motionVideoPath.value = null
                        motionExoPlayer?.stop()
                        motionExoPlayer?.clearMediaItems()
                        currentLoadedMotionPath = null
                    }
                } else if (_motionVideoPath.value == null) {
                    _playbackState.value.currentTrack?.let { current ->
                        fetchMotionArtworkForTrack(current)
                    }
                }
            }
        }
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    fun getMediaSession(): MediaSession? = mediaSession

    fun attachMediaSession(session: MediaSession?) {
        this.mediaSession = session
    }

    fun startPlaybackService() {
        try {
            val intent = Intent(context, OneMusicPlaybackService::class.java)
            context.startService(intent)
        } catch (_: Exception) {}
    }

    private fun setupPlayer() {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(headroomLimiter))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        // 1. Accurate MP3, AAC, and FLAC Frame Index Seeking
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(
                Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING or Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING
            )
            .setFlacExtractorFlags(
                FlacExtractor.FLAG_DISABLE_ID3_METADATA
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        // 2. High-Performance Local Audio Load Control (Fast seek & responsive buffering)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 3_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 3. Professional Media Audio Attributes & Audio Focus Handling
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        val isActivelyPlaying = isPlaying || (exoPlayer?.playWhenReady == true && exoPlayer?.playbackState != Player.STATE_ENDED && exoPlayer?.playbackState != Player.STATE_IDLE)
                        _playbackState.value = _playbackState.value.copy(isPlaying = isActivelyPlaying)
                        if (isActivelyPlaying) {
                            startProgressTracker()
                            startPlaybackService()
                        } else {
                            stopProgressTracker()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (isReorderingQueue) {
                            // Sắp xếp thứ tự hàng đợi, không phải chuyển bài hát mới. Bỏ qua để tránh reset playback và nháy UI
                            return
                        }
                        consecutivePlaybackErrors = 0
                        // Gapless playback transition handling: seamlessly switch track metadata without re-buffering
                        val mediaId = mediaItem?.mediaId ?: return
                        val queue = _playbackState.value.queue
                        val playerIndex = exoPlayer?.currentMediaItemIndex ?: -1
                        val newIndex = if (playerIndex in queue.indices && queue[playerIndex].id == mediaId) {
                            playerIndex
                        } else {
                            queue.indexOfFirst { it.id == mediaId }
                        }
                        if (newIndex != -1) {
                            val track = queue[newIndex]
                            _playbackState.value = _playbackState.value.copy(
                                currentIndex = newIndex,
                                currentTrack = track,
                                currentPositionMs = 0L,
                                durationMs = track.durationMs,
                                activeGainDb = track.replayGainDb ?: 0.0f,
                                hasReplayGain = track.replayGainDb != null,
                                replayGainOrigin = track.replayGainOrigin
                            )
                            _positionMs.value = 0L
                            updateReplayGainForTrack(track)
                            loadLyricsIfMissing(track)
                            preloadSurroundingTracks(queue, newIndex)
                            settingsPreferences.saveLastPlaybackState(track.id, 0L)
                            settingsPreferences.addRecentlyPlayedTrack(track.id)
                            fetchMotionArtworkForTrack(track)

                            val settings = settingsPreferences.getSettings()
                            if (settings.isCrossfadeEnabled) {
                                smoothFadeIn(durationMs = (settings.crossfadeDurationSeconds * 500L).coerceIn(500L, 2500L))
                            } else if (!settings.isGaplessPlaybackEnabled && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                                smoothFadeIn(durationMs = 250L)
                            } else {
                                exoPlayer?.volume = userVolume
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            consecutivePlaybackErrors = 0
                            val duration = exoPlayer?.duration ?: 0L
                            _playbackState.value = _playbackState.value.copy(
                                durationMs = if (duration > 0) duration else _playbackState.value.currentTrack?.durationMs ?: 0L
                            )
                            exoPlayer?.audioSessionId?.let { sessionId ->
                                audioEffectManager.attachAudioSession(sessionId)
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            handleTrackEnded()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("MusicPlayerController", "Playback error intercepted: ${error.errorCodeName} - ${error.message}")
                        consecutivePlaybackErrors++
                        val queueSize = _playbackState.value.queue.size
                        if (consecutivePlaybackErrors < queueSize && queueSize > 1) {
                            scope.launch {
                                try {
                                    android.widget.Toast.makeText(context, "Không thể phát tệp âm thanh này, đang chuyển bài...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (_: Throwable) {}
                                skipToNext(ignoreThrottle = true)
                            }
                        } else {
                            consecutivePlaybackErrors = 0
                            _playbackState.value = _playbackState.value.copy(isPlaying = false)
                            try {
                                exoPlayer?.stop()
                            } catch (_: Throwable) {}
                        }
                    }
                })
            }

        exoPlayer = player
        try {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            mediaSession = MediaSession.Builder(context, player)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        } catch (_: Exception) {}
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        val artworkUri = if (track.artworkUrl.isNotBlank()) Uri.parse(track.artworkUrl) else null
        val audioUri = Uri.parse(track.audioUrl)
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(audioUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
    }

    private fun updateReplayGainForTrack(track: Track?) {
        val settings = settingsPreferences.getSettings()
        val targetGain = if (settings.isReplayGainEnabled) (track?.replayGainDb ?: 0.0f) else 0.0f
        headroomLimiter.setTargetGainDb(targetGain)
        headroomLimiter.setEnabled(settings.isHeadroomLimiterEnabled)
        _playbackState.value = _playbackState.value.copy(
            activeGainDb = targetGain,
            hasReplayGain = settings.isReplayGainEnabled && track?.replayGainDb != null,
            replayGainOrigin = if (settings.isReplayGainEnabled) track?.replayGainOrigin else null
        )
    }

    private fun loadLyricsIfMissing(track: Track) {
        if (track.lyrics.isNotEmpty()) return
        if (!settingsPreferences.getSettings().isOnlineLyricsEnabled) return
        lyricsJob?.cancel()
        lyricsJob = scope.launch(Dispatchers.IO) {
            val fetchedLyrics = LrclibLyricsProvider.getLyrics(context, track)
            if (fetchedLyrics.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val current = _playbackState.value.currentTrack
                    if (current?.id == track.id) {
                        val updatedTrack = current.copy(lyrics = fetchedLyrics)
                        val updatedQueue = _playbackState.value.queue.map {
                            if (it.id == track.id) updatedTrack else it
                        }
                        _playbackState.value = _playbackState.value.copy(
                            currentTrack = updatedTrack,
                            queue = updatedQueue
                        )
                    }
                }
            }
        }
    }

    private var preloadJob: Job? = null
    private val inMemoryMotionCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun preloadSurroundingTracks(queue: List<Track>, currentIndex: Int) {
        if (queue.isEmpty()) return
        preloadJob?.cancel()
        preloadJob = scope.launch(Dispatchers.IO) {
            val nextIndex = (currentIndex + 1) % queue.size
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else queue.lastIndex

            val nextTrack = queue.getOrNull(nextIndex)
            val prevTrack = queue.getOrNull(prevIndex)

            nextTrack?.artworkUrl?.let { com.example.onemusic.ui.utils.preloadArtworkAndColors(context, it) }
            if (prevTrack?.id != nextTrack?.id) {
                prevTrack?.artworkUrl?.let { com.example.onemusic.ui.utils.preloadArtworkAndColors(context, it) }
            }

            // Predictive Motion Artwork Preload: download next & prev song's video before it plays
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
    }

    /**
     * Fetch animated/motion album artwork for a track.
     * Checks fast in-memory cache first (0ms), then local disk cache, then Apple Music.
     * Updates motionVideoPath StateFlow for UI to observe.
     */
    private fun fetchMotionArtworkForTrack(track: Track) {
        motionJob?.cancel()

        // If Motion Artwork is disabled by user in Settings, clear and exit immediately
        if (!settingsPreferences.getSettings().isMotionArtworkEnabled) {
            _motionVideoPath.value = null
            motionExoPlayer?.stop()
            motionExoPlayer?.clearMediaItems()
            currentLoadedMotionPath = null
            return
        }

        // 1. Kiểm tra In-Memory Cache tức thì (0ms) bằng track ID hoặc Artist+Album (không bao giờ dùng albumName trần)
        val cleanAlbum = track.album.lowercase().trim()
        val cleanArtist = track.artist.lowercase().trim()
        val isGeneric = com.example.onemusic.data.scanner.AppleMusicMotionFetcher.isGenericAlbum(cleanAlbum) ||
                com.example.onemusic.data.scanner.AppleMusicMotionFetcher.isGenericArtist(cleanArtist)
        val artistAlbumKey = if (!isGeneric) "${cleanArtist}_${cleanAlbum}" else null

        val memCached = inMemoryMotionCache[track.id]
            ?: (if (artistAlbumKey != null) inMemoryMotionCache[artistAlbumKey] else null)

        if (memCached != null && File(memCached).exists() && File(memCached).length() > 0) {
            _motionVideoPath.value = memCached
            updateMotionPlayerMedia(memCached)
            android.util.Log.d("MotionArt", "⚡ Instant in-memory cache hit (0ms): $memCached")
            return
        }

        // 2. Nếu chưa có trong RAM, RESET NGAY _motionVideoPath để không bao giờ bị kẹt ảnh của bài hát trước
        _motionVideoPath.value = null
        motionExoPlayer?.stop()
        motionExoPlayer?.clearMediaItems()
        currentLoadedMotionPath = null

        android.util.Log.d("MotionArt", "🎬 Fetching motion art for: ${track.title} - ${track.artist}")
        motionJob = scope.launch(Dispatchers.IO) {
            val cachedPath = musicRepository.getLocalMotionVideoPath(track)
            if (cachedPath != null) {
                inMemoryMotionCache[track.id] = cachedPath
                if (artistAlbumKey != null) inMemoryMotionCache[artistAlbumKey] = cachedPath
                android.util.Log.d("MotionArt", "✅ Disk cache hit! Video at: $cachedPath")
                withContext(Dispatchers.Main) {
                    if (_playbackState.value.currentTrack?.id == track.id) {
                        _motionVideoPath.value = cachedPath
                        updateMotionPlayerMedia(cachedPath)
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
                        if (_playbackState.value.currentTrack?.id == track.id) {
                            _motionVideoPath.value = streamOrLocal
                            updateMotionPlayerMedia(streamOrLocal)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (_playbackState.value.currentTrack?.id == track.id) {
                        _motionVideoPath.value = null
                        motionExoPlayer?.stop()
                        motionExoPlayer?.clearMediaItems()
                        currentLoadedMotionPath = null
                    }
                }
            }
        }
    }

    /**
     * Remove motion artwork for the currently playing track and update UI immediately.
     */
    fun removeMotionArtworkForCurrentTrack() {
        val track = _playbackState.value.currentTrack ?: return
        scope.launch(Dispatchers.IO) {
            musicRepository.removeMotionArtworkForTrack(track)
            inMemoryMotionCache.remove(track.id)
            val cleanAlbum = track.album.lowercase().trim()
            val cleanArtist = track.artist.lowercase().trim()
            inMemoryMotionCache.remove("${cleanArtist}_${cleanAlbum}")
            withContext(Dispatchers.Main) {
                _motionVideoPath.value = null
                motionExoPlayer?.stop()
                motionExoPlayer?.clearMediaItems()
                currentLoadedMotionPath = null
            }
        }
    }

    /**
     * Completely wipe all motion artwork cache from RAM and disk.
     */
    fun clearMotionArtworkCache() {
        scope.launch(Dispatchers.IO) {
            inMemoryMotionCache.clear()
            musicRepository.clearAllMotionArtworkCache()
            withContext(Dispatchers.Main) {
                _motionVideoPath.value = null
                motionExoPlayer?.stop()
                motionExoPlayer?.clearMediaItems()
                currentLoadedMotionPath = null
            }
        }
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        val player = exoPlayer ?: return

        // Đang shuffle: lưu thứ tự gốc, đưa bài được chọn lên đầu và xáo phần còn lại
        val isShuffling = _playbackState.value.isShuffle
        val startInOriginal = startIndex.coerceIn(0, tracks.lastIndex)
        val playOrder: List<Track>
        val clampedIndex: Int
        if (isShuffling) {
            originalQueue = tracks.toMutableList()
            val rest = tracks.filterIndexed { i, _ -> i != startInOriginal }.shuffled()
            playOrder = listOf(tracks[startInOriginal]) + rest
            clampedIndex = 0
        } else {
            originalQueue = null
            playOrder = tracks
            clampedIndex = startInOriginal
        }
        @Suppress("NAME_SHADOWING")
        val tracks = playOrder

        // Smart Queue Reuse: if the new tracks list matches the existing queue in size and keys,
        // and player already has items loaded, avoid destroying & rebuilding the entire ExoPlayer queue!
        // (Bỏ qua khi shuffle: thứ tự vừa xáo mới luôn khác hàng đợi hiện tại)
        val currentQueue = _playbackState.value.queue
        val isSameQueue = !isShuffling &&
                currentQueue.size == tracks.size &&
                player.mediaItemCount == tracks.size &&
                currentQueue.firstOrNull()?.id == tracks.firstOrNull()?.id &&
                currentQueue.lastOrNull()?.id == tracks.lastOrNull()?.id &&
                currentQueue.getOrNull(clampedIndex)?.id == tracks[clampedIndex].id

        if (isSameQueue) {
            switchTrackTo(clampedIndex, autoPlay = autoPlay)
            return
        }

        val track = tracks[clampedIndex]
        _playbackState.value = _playbackState.value.copy(
            queue = tracks,
            currentIndex = clampedIndex,
            currentTrack = track,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            isPlaying = autoPlay,
            activeGainDb = track.replayGainDb ?: 0.0f,
            hasReplayGain = track.replayGainDb != null,
            replayGainOrigin = track.replayGainOrigin,
            transitionDirection = 0
        )
        _positionMs.value = 0L
        updateReplayGainForTrack(track)
        loadLyricsIfMissing(track)
        preloadSurroundingTracks(tracks, clampedIndex)
        fetchMotionArtworkForTrack(track)

        val mediaItems = tracks.map { trackToMediaItem(it) }
        player.setMediaItems(mediaItems, clampedIndex, 0L)
        player.prepare()

        if (autoPlay) {
            smoothFadeIn()
        } else {
            player.playWhenReady = false
            player.volume = userVolume
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    fun playTrack(track: Track, autoPlay: Boolean = true) {
        val player = exoPlayer ?: return
        val queue = _playbackState.value.queue
        val existingIndex = queue.indexOfFirst { it.id == track.id }

        if (existingIndex != -1 && existingIndex < player.mediaItemCount) {
            switchTrackTo(existingIndex, autoPlay = autoPlay)
        } else {
            setQueue(listOf(track), startIndex = 0, autoPlay = autoPlay)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            smoothFadeOut {
                player.pause()
                player.volume = userVolume
            }
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED || player.mediaItemCount == 0) {
                val current = _playbackState.value.currentTrack
                if (current != null) {
                    playTrack(current, autoPlay = true)
                } else if (_playbackState.value.queue.isNotEmpty()) {
                    setQueue(_playbackState.value.queue, startIndex = 0, autoPlay = true)
                }
            } else {
                player.volume = userVolume
                smoothFadeIn()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val totalDur = if (player.duration > 0) player.duration else (_playbackState.value.durationMs)
        val safePos = if (totalDur > 1000L) {
            positionMs.coerceIn(0L, totalDur - 500L)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        player.seekTo(safePos)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = safePos)
        _positionMs.value = safePos
    }

    private var trackSwitchJob: Job? = null

    private fun switchTrackTo(index: Int, autoPlay: Boolean = true) {
        val state = _playbackState.value
        if (index !in state.queue.indices) return
        val player = exoPlayer ?: return

        trackSwitchJob?.cancel()
        volumeFader.cancel()

        val track = state.queue[index]
        val wasPlaying = player.isPlaying || player.playWhenReady

        // 1. Immediately update UI state (title, cover, duration update instantly and isPlaying never jitters)
        _playbackState.value = state.copy(
            currentIndex = index,
            currentTrack = track,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            isPlaying = wasPlaying || autoPlay,
            activeGainDb = track.replayGainDb ?: 0.0f,
            hasReplayGain = track.replayGainDb != null,
            replayGainOrigin = track.replayGainOrigin
        )
        _positionMs.value = 0L
        updateReplayGainForTrack(track)
        loadLyricsIfMissing(track)
        preloadSurroundingTracks(state.queue, index)
        fetchMotionArtworkForTrack(track)

        val settings = settingsPreferences.getSettings()

        // 2. Immediately command ExoPlayer to seek to the new track so internal position resets to 0L instantly
        if (index < player.mediaItemCount) {
            player.seekToDefaultPosition(index)
        } else {
            val mediaItems = state.queue.map { trackToMediaItem(it) }
            player.setMediaItems(mediaItems, index, 0L)
            player.prepare()
        }

        // 3. Smooth volume ramp-up (fade in) for seamless audio transition without pops or stutters
        if (wasPlaying || autoPlay) {
            val fadeInDur = if (settings.isCrossfadeEnabled) {
                (settings.crossfadeDurationSeconds * 400L).coerceIn(350L, 2500L)
            } else {
                120L
            }
            smoothFadeIn(durationMs = fadeInDur)
        } else {
            player.volume = userVolume
        }
    }

    private var lastUserSkipTimestamp: Long = 0L
    private val skipThrottleMs = 350L

    fun skipToNext(ignoreThrottle: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!ignoreThrottle && now - lastUserSkipTimestamp < skipThrottleMs) return
        if (!ignoreThrottle) lastUserSkipTimestamp = now

        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        // state.queue đã là thứ tự phát thật (kể cả khi shuffle) → luôn phát bài kế tiếp trong danh sách
        val nextIndex = (state.currentIndex + 1) % state.queue.size
        switchTrackTo(nextIndex, autoPlay = true)
    }

    fun skipToPrevious(ignoreThrottle: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!ignoreThrottle && now - lastUserSkipTimestamp < skipThrottleMs) return
        if (!ignoreThrottle) lastUserSkipTimestamp = now

        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        val player = exoPlayer ?: return

        if (player.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.lastIndex
        switchTrackTo(prevIndex, autoPlay = true)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        userVolume = clamped
        exoPlayer?.volume = clamped
    }

    fun playQueueIndex(index: Int) {
        switchTrackTo(index, autoPlay = true)
    }

    fun addToQueue(track: Track) {
        val state = _playbackState.value
        if (state.queue.isEmpty()) {
            setQueue(listOf(track), startIndex = 0, autoPlay = false)
            return
        }
        val updatedQueue = state.queue + track
        _playbackState.value = state.copy(queue = updatedQueue)
        exoPlayer?.addMediaItem(trackToMediaItem(track))
        // Đang shuffle: bài "thêm vào hàng đợi" cũng nối vào cuối thứ tự gốc
        originalQueue?.add(track)
    }

    fun playNext(track: Track) {
        val state = _playbackState.value
        if (state.queue.isEmpty()) {
            setQueue(listOf(track), startIndex = 0, autoPlay = false)
            return
        }
        val insertIndex = (state.currentIndex + 1).coerceIn(0, state.queue.size)
        val mutableQueue = state.queue.toMutableList()
        mutableQueue.add(insertIndex, track)
        _playbackState.value = state.copy(queue = mutableQueue)
        exoPlayer?.addMediaItem(insertIndex, trackToMediaItem(track))
        insertAfterCurrentInOriginal(listOf(track), state.currentTrack)
    }

    fun playNextTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val state = _playbackState.value
        if (state.queue.isEmpty()) {
            setQueue(tracks, startIndex = 0, autoPlay = false)
            return
        }
        val insertIndex = (state.currentIndex + 1).coerceIn(0, state.queue.size)
        val mutableQueue = state.queue.toMutableList()
        mutableQueue.addAll(insertIndex, tracks)
        _playbackState.value = state.copy(queue = mutableQueue)
        val mediaItems = tracks.map { trackToMediaItem(it) }
        exoPlayer?.addMediaItems(insertIndex, mediaItems)
        insertAfterCurrentInOriginal(tracks, state.currentTrack)
    }

    /** Đang shuffle: bài "Phát tiếp" được đặt ngay sau bài đang phát trong thứ tự gốc. */
    private fun insertAfterCurrentInOriginal(tracks: List<Track>, currentTrack: Track?) {
        val original = originalQueue ?: return
        val currentPos = original.indexOfFirst { it.id == currentTrack?.id }
        val insertAt = if (currentPos >= 0) currentPos + 1 else original.size
        original.addAll(insertAt, tracks)
    }

    /** Đang shuffle: xóa 1 lần xuất hiện của bài khỏi thứ tự gốc để khi tắt shuffle không hiện lại. */
    private fun removeFromOriginal(track: Track) {
        val original = originalQueue ?: return
        val pos = original.indexOfFirst { it.id == track.id }
        if (pos >= 0) original.removeAt(pos)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        val queue = state.queue
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return

        val mutableQueue = queue.toMutableList()
        val item = mutableQueue.removeAt(fromIndex)
        mutableQueue.add(toIndex, item)

        val newCurrentIndex = when {
            state.currentIndex == fromIndex -> toIndex
            fromIndex < state.currentIndex && toIndex >= state.currentIndex -> state.currentIndex - 1
            fromIndex > state.currentIndex && toIndex <= state.currentIndex -> state.currentIndex + 1
            else -> state.currentIndex
        }

        _playbackState.value = state.copy(
            queue = mutableQueue,
            currentIndex = newCurrentIndex
        )
        isReorderingQueue = true
        try {
            exoPlayer?.moveMediaItem(fromIndex, toIndex)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerController", "Error moving queue item: ${e.message}")
        } finally {
            isReorderingQueue = false
        }
    }

    fun removeQueueItem(index: Int) {
        val state = _playbackState.value
        val queue = state.queue
        if (index !in queue.indices || queue.isEmpty()) return
        removeFromOriginal(queue[index])

        if (queue.size == 1) {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            _playbackState.value = state.copy(
                currentTrack = null,
                queue = emptyList(),
                currentIndex = -1,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = 0L
            )
            _positionMs.value = 0L
            return
        }

        val mutableQueue = queue.toMutableList()
        mutableQueue.removeAt(index)

        val newCurrentIndex = when {
            index < state.currentIndex -> state.currentIndex - 1
            index == state.currentIndex -> index.coerceAtMost(mutableQueue.lastIndex)
            else -> state.currentIndex
        }

        val newCurrentTrack = if (index == state.currentIndex) {
            mutableQueue.getOrNull(newCurrentIndex)
        } else {
            state.currentTrack
        }

        _playbackState.value = state.copy(
            queue = mutableQueue,
            currentIndex = newCurrentIndex,
            currentTrack = newCurrentTrack
        )
        isReorderingQueue = true
        try {
            exoPlayer?.removeMediaItem(index)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerController", "Error removing queue item: ${e.message}")
        } finally {
            isReorderingQueue = false
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
        exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(clamped, 1.0f)
    }

    fun restoreLastPlaybackState(availableTracks: List<Track>) {
        if (availableTracks.isEmpty()) return
        if (_playbackState.value.currentTrack != null) return
        val lastTrackId = settingsPreferences.getSettings().lastPlayedTrackId ?: return
        val lastPos = settingsPreferences.getSettings().lastPlayedPositionMs
        val trackIndex = availableTracks.indexOfFirst { it.id == lastTrackId }
        if (trackIndex != -1) {
            setQueue(availableTracks, startIndex = trackIndex, autoPlay = false)
            if (lastPos > 0L) {
                seekTo(lastPos)
            }
        }
    }

    fun updateTrackFavorite(trackId: String, isFavorite: Boolean) {
        val state = _playbackState.value
        val updatedCurrent = if (state.currentTrack?.id == trackId) {
            state.currentTrack.copy(isFavorite = isFavorite)
        } else {
            state.currentTrack
        }
        val updatedQueue = state.queue.map {
            if (it.id == trackId) it.copy(isFavorite = isFavorite) else it
        }
        originalQueue?.replaceAll { if (it.id == trackId) it.copy(isFavorite = isFavorite) else it }
        _playbackState.value = state.copy(
            currentTrack = updatedCurrent,
            queue = updatedQueue
        )
    }

    /**
     * Shuffle kiểu Apple Music: xáo trực tiếp state.queue (thứ tự hiển thị = thứ tự phát),
     * KHÔNG dùng exoPlayer.shuffleModeEnabled (vì ExoPlayer xáo ngầm bên trong, lệch với giao diện).
     * - Bật: giữ nguyên lịch sử + bài đang phát, xáo phần phía sau; lưu thứ tự gốc vào originalQueue.
     * - Tắt: khôi phục originalQueue, tìm lại bài đang phát theo id.
     * Danh sách media của ExoPlayer được sắp lại bằng moveMediaItem nên bài đang phát không bị ngắt.
     */
    fun toggleShuffle() {
        val state = _playbackState.value
        val newShuffle = !state.isShuffle
        exoPlayer?.shuffleModeEnabled = false
        val queue = state.queue
        val currentIndex = state.currentIndex

        if (newShuffle) {
            originalQueue = queue.toMutableList()
            if (currentIndex in queue.indices && currentIndex + 1 < queue.size) {
                val head = queue.subList(0, currentIndex + 1)
                val tail = queue.subList(currentIndex + 1, queue.size).shuffled()
                val newQueue = head + tail
                _playbackState.value = state.copy(isShuffle = true, queue = newQueue)
                reorderPlayerMediaItems(queue, newQueue, currentIndex)
            } else {
                _playbackState.value = state.copy(isShuffle = true)
            }
        } else {
            val original = originalQueue
            originalQueue = null
            if (original == null || original.size != queue.size || queue.isEmpty()) {
                // Không có thứ tự gốc hợp lệ (không nên xảy ra) → giữ nguyên thứ tự hiện tại
                _playbackState.value = state.copy(isShuffle = false)
                return
            }
            val currentId = state.currentTrack?.id
            val restoredIndex = original.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: currentIndex
            _playbackState.value = state.copy(
                isShuffle = false,
                queue = original.toList(),
                currentIndex = restoredIndex
            )
            reorderPlayerMediaItems(queue, original, restoredIndex)
        }
    }

    /**
     * Sắp lại danh sách media của ExoPlayer từ thứ tự [from] sang [to] bằng moveMediaItem
     * (không ngắt bài đang phát). Nếu player lệch số lượng bài thì nạp lại toàn bộ và giữ vị trí phát.
     */
    private fun reorderPlayerMediaItems(from: List<Track>, to: List<Track>, newCurrentIndex: Int) {
        val player = exoPlayer ?: return
        if (player.mediaItemCount == 0) return
        isReorderingQueue = true
        try {
            if (player.mediaItemCount != from.size || from.size != to.size) {
                val wasPlaying = player.playWhenReady
                player.setMediaItems(to.map { trackToMediaItem(it) }, newCurrentIndex.coerceIn(0, to.lastIndex), player.currentPosition)
                player.prepare()
                player.playWhenReady = wasPlaying
                return
            }
            val working = from.toMutableList()
            for (i in to.indices) {
                if (working[i].id == to[i].id) continue
                var j = i + 1
                while (j < working.size && working[j].id != to[i].id) j++
                if (j >= working.size) continue
                player.moveMediaItem(j, i)
                working.add(i, working.removeAt(j))
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerController", "Error reordering queue for shuffle: ${e.message}")
        } finally {
            isReorderingQueue = false
        }
    }

    fun cycleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
        exoPlayer?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun toggleAutoplay() {
        val newAutoplay = !_playbackState.value.isAutoplay
        _playbackState.value = _playbackState.value.copy(isAutoplay = newAutoplay)
    }

    fun clearPlaybackHistory() {
        val state = _playbackState.value
        val currentIndex = state.currentIndex
        if (currentIndex <= 0 || state.queue.isEmpty()) return

        val newQueue = state.queue.subList(currentIndex, state.queue.size).toList()
        state.queue.subList(0, currentIndex).forEach { removeFromOriginal(it) }
        _playbackState.value = state.copy(
            queue = newQueue,
            currentIndex = 0
        )
        isReorderingQueue = true
        try {
            exoPlayer?.removeMediaItems(0, currentIndex)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerController", "Error clearing playback history: ${e.message}")
        } finally {
            isReorderingQueue = false
        }
    }

    fun pause() {
        val player = exoPlayer ?: return
        _playbackState.value.currentTrack?.let { track ->
            settingsPreferences.saveLastPlaybackState(track.id, _positionMs.value)
        }
        if (player.isPlaying) {
            smoothFadeOut {
                player.pause()
                player.volume = userVolume
            }
        }
    }

    fun setSleepTimer(minutes: Int) = sleepTimer.set(minutes)

    fun setSleepTimerEndOfTrack() = sleepTimer.setEndOfTrack()

    fun cancelSleepTimer() = sleepTimer.cancel()

    private fun handleTrackEnded() {
        val state = _playbackState.value
        if (state.sleepTimerMinutes == -1) {
            cancelSleepTimer()
            exoPlayer?.pause()
            _playbackState.value = state.copy(isPlaying = false, currentPositionMs = 0L)
            _positionMs.value = 0L
            return
        }
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                smoothFadeIn()
            }
            RepeatMode.ALL -> {
                skipToNext(ignoreThrottle = true)
            }
            RepeatMode.OFF -> {
                if (state.currentIndex < state.queue.lastIndex) {
                    skipToNext(ignoreThrottle = true)
                } else if (state.isAutoplay && state.queue.isNotEmpty()) {
                    skipToNext(ignoreThrottle = true)
                } else {
                    _playbackState.value = state.copy(isPlaying = false, currentPositionMs = 0L)
                    _positionMs.value = 0L
                }
            }
        }
    }

    // --- Smart Fade In / Fade Out (Smooth Transitions & Crossfade) ---
    private fun smoothFadeIn(durationMs: Long = 150L) = volumeFader.fadeIn(durationMs)

    private fun smoothFadeOut(durationMs: Long = 150L, onComplete: () -> Unit) = volumeFader.fadeOut(durationMs, onComplete)

    private fun startProgressTracker() = positionTracker.start()

    private fun stopProgressTracker() = positionTracker.stop()

    fun release() {
        _playbackState.value.currentTrack?.let { track ->
            settingsPreferences.saveLastPlaybackState(track.id, _positionMs.value)
        }
        volumeFader.cancel()
        lyricsJob?.cancel()
        motionJob?.cancel()
        preloadJob?.cancel()
        stopProgressTracker()
        audioOutputManager.release()
        try { mediaSession?.release() } catch (_: Exception) {}
        mediaSession = null
        try { exoPlayer?.release() } catch (_: Exception) {}
        exoPlayer = null
        try { motionExoPlayer?.release() } catch (_: Exception) {}
        motionExoPlayer = null
        audioEffectManager.release()
        synchronized(MusicPlayerController::class.java) {
            if (instance === this) {
                instance = null
            }
        }
    }
}
