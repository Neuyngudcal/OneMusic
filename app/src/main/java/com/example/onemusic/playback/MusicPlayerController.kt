package com.example.onemusic.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.onemusic.MainActivity
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.motion.MotionArtworkController
import com.example.onemusic.playback.player.ExoPlayerFactory
import com.example.onemusic.playback.player.PlayerEventListener
import com.example.onemusic.playback.queue.QueueOperations
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
    private var userVolume: Float = 1.0f
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

    // Bìa động (video bìa) – player riêng, bộ nhớ đệm và việc tải nằm trong MotionArtworkController
    private val motionArtwork = MotionArtworkController(
        context = context,
        scope = scope,
        musicRepository = musicRepository,
        settingsPreferences = settingsPreferences,
        currentTrackId = { _playbackState.value.currentTrack?.id }
    )

    // ReplayGain, tải lời, nạp trước bài kế/trước khi đổi bài
    private val trackPreparation = TrackPreparation(
        context = context,
        scope = scope,
        settingsPreferences = settingsPreferences,
        headroomLimiter = headroomLimiter,
        playbackState = _playbackState,
        motionArtwork = motionArtwork
    )

    // Motion Artwork State: Local video path for animated album art on Now Playing
    val motionVideoPath: StateFlow<String?> = motionArtwork.motionVideoPath

    // Dedicated Pre-warmed Motion Artwork Player instance (Single reusable instance for 0ms instant playback)
    val motionExoPlayer: ExoPlayer?
        get() = motionArtwork.player

    // Chỉ cho phép giải mã video bìa động khi Now Playing đang hiển thị (tránh hao pin khi chạy nền)
    fun setMotionPlaybackAllowed(allowed: Boolean) = motionArtwork.setPlaybackAllowed(allowed)

    /** Việc controller làm khi ExoPlayer báo sự kiện (xem player/PlayerEventListener.kt). */
    private val playerEventCallbacks = object : PlayerEventListener.Callbacks {
        override val isReorderingQueue: Boolean
            get() = this@MusicPlayerController.isReorderingQueue

        override fun onActivePlaybackChanged(isActivelyPlaying: Boolean) {
            if (isActivelyPlaying) {
                startProgressTracker()
                startPlaybackService()
            } else {
                stopProgressTracker()
            }
        }

        override fun onCurrentTrackChanged(track: Track, index: Int, queue: List<Track>, reason: Int) {
            _playbackState.value = _playbackState.value.copy(
                currentIndex = index,
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
            preloadSurroundingTracks(queue, index)
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

        override fun onPlayerReady(audioSessionId: Int) {
            audioEffectManager.attachAudioSession(audioSessionId)
        }

        override fun onTrackEnded() {
            handleTrackEnded()
        }

        override fun onSkipAfterError() {
            scope.launch {
                try {
                    android.widget.Toast.makeText(context, "Không thể phát tệp âm thanh này, đang chuyển bài...", android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {}
                skipToNext(ignoreThrottle = true)
            }
        }
    }

    init {
        setupPlayer()
        motionArtwork.setupPlayer()
        motionArtwork.preloadCacheFromDb()
        setupHeadsetCallbacks()
        setupSettingsObserver()
        setupWidgetObserver()
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
                motionArtwork.onSettingsChanged(settings.isMotionArtworkEnabled, _playbackState.value.currentTrack)
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
        val player = ExoPlayerFactory.createMusicPlayer(context, headroomLimiter).apply {
            addListener(PlayerEventListener(
                player = { exoPlayer },
                playbackState = _playbackState,
                callbacks = playerEventCallbacks
            ))
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

    private fun updateReplayGainForTrack(track: Track?) = trackPreparation.updateReplayGain(track)

    private fun loadLyricsIfMissing(track: Track) = trackPreparation.loadLyricsIfMissing(track)

    private fun preloadSurroundingTracks(queue: List<Track>, currentIndex: Int) =
        trackPreparation.preloadSurroundingTracks(queue, currentIndex)

    private fun fetchMotionArtworkForTrack(track: Track) = motionArtwork.fetchForTrack(track)

    /**
     * Remove motion artwork for the currently playing track and update UI immediately.
     */
    fun removeMotionArtworkForCurrentTrack() {
        val track = _playbackState.value.currentTrack ?: return
        motionArtwork.removeForTrack(track)
    }

    /**
     * Completely wipe all motion artwork cache from RAM and disk.
     */
    fun clearMotionArtworkCache() = motionArtwork.clearCache()

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
            playOrder = QueueOperations.shuffledPlayOrder(tracks, startInOriginal)
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
        val result = QueueOperations.append(state.queue, state.currentIndex, track)
        _playbackState.value = state.copy(queue = result.queue)
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
        val insertIndex = QueueOperations.insertIndexAfterCurrent(state.queue, state.currentIndex)
        val result = QueueOperations.insertNext(state.queue, state.currentIndex, listOf(track))
        _playbackState.value = state.copy(queue = result.queue)
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
        val insertIndex = QueueOperations.insertIndexAfterCurrent(state.queue, state.currentIndex)
        val result = QueueOperations.insertNext(state.queue, state.currentIndex, tracks)
        _playbackState.value = state.copy(queue = result.queue)
        val mediaItems = tracks.map { trackToMediaItem(it) }
        exoPlayer?.addMediaItems(insertIndex, mediaItems)
        insertAfterCurrentInOriginal(tracks, state.currentTrack)
    }

    /** Đang shuffle: bài "Phát tiếp" được đặt ngay sau bài đang phát trong thứ tự gốc. */
    private fun insertAfterCurrentInOriginal(tracks: List<Track>, currentTrack: Track?) {
        val original = originalQueue ?: return
        QueueOperations.insertAfterCurrentInOriginal(original, tracks, currentTrack?.id)
    }

    /** Đang shuffle: xóa 1 lần xuất hiện của bài khỏi thứ tự gốc để khi tắt shuffle không hiện lại. */
    private fun removeFromOriginal(track: Track) {
        val original = originalQueue ?: return
        QueueOperations.removeFromOriginal(original, track)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val state = _playbackState.value
        val result = QueueOperations.move(state.queue, state.currentIndex, fromIndex, toIndex) ?: return

        _playbackState.value = state.copy(
            queue = result.queue,
            currentIndex = result.currentIndex
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
        val result = QueueOperations.remove(queue, state.currentIndex, index) ?: return
        removeFromOriginal(queue[index])

        if (result.queue.isEmpty()) {
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

        val newCurrentTrack = if (index == state.currentIndex) {
            result.queue.getOrNull(result.currentIndex)
        } else {
            state.currentTrack
        }

        _playbackState.value = state.copy(
            queue = result.queue,
            currentIndex = result.currentIndex,
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
            val newQueue = QueueOperations.shuffleUpcoming(queue, currentIndex)
            if (newQueue != null) {
                _playbackState.value = state.copy(isShuffle = true, queue = newQueue)
                reorderPlayerMediaItems(queue, newQueue, currentIndex)
            } else {
                _playbackState.value = state.copy(isShuffle = true)
            }
        } else {
            val original = originalQueue
            originalQueue = null
            val restored = QueueOperations.restoreOriginal(original, queue, state.currentTrack?.id, currentIndex)
            if (restored == null) {
                // Không có thứ tự gốc hợp lệ (không nên xảy ra) → giữ nguyên thứ tự hiện tại
                _playbackState.value = state.copy(isShuffle = false)
                return
            }
            _playbackState.value = state.copy(
                isShuffle = false,
                queue = restored.queue,
                currentIndex = restored.currentIndex
            )
            reorderPlayerMediaItems(queue, restored.queue, restored.currentIndex)
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
            for ((moveFrom, moveTo) in QueueOperations.reorderMoves(from, to)) {
                player.moveMediaItem(moveFrom, moveTo)
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
        val result = QueueOperations.clearHistory(state.queue, currentIndex) ?: return

        state.queue.subList(0, currentIndex).forEach { removeFromOriginal(it) }
        _playbackState.value = state.copy(
            queue = result.queue,
            currentIndex = result.currentIndex
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
        trackPreparation.cancelLyrics()
        motionArtwork.cancelPendingFetch()
        trackPreparation.cancelPreload()
        stopProgressTracker()
        audioOutputManager.release()
        try { mediaSession?.release() } catch (_: Exception) {}
        mediaSession = null
        try { exoPlayer?.release() } catch (_: Exception) {}
        exoPlayer = null
        motionArtwork.releasePlayer()
        audioEffectManager.release()
        synchronized(MusicPlayerController::class.java) {
            if (instance === this) {
                instance = null
            }
        }
    }
}
