package com.example.onemusic.playback

import android.content.Context
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.LrclibLyricsProvider
import com.example.onemusic.playback.motion.MotionArtworkController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Chuẩn bị khi một bài trở thành bài đang phát: áp ReplayGain / Headroom Limiter,
 * tải lời bài hát từ Internet nếu tệp không có, nạp trước ảnh bìa + bìa động của bài kế/trước.
 * Sở hữu `lyricsJob` và `preloadJob`; `playbackState` do [MusicPlayerController] sở hữu, lớp này chỉ ghi vào.
 */
internal class TrackPreparation(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsPreferences: SettingsPreferences,
    private val headroomLimiter: HeadroomLimiterAudioProcessor,
    private val playbackState: MutableStateFlow<PlaybackState>,
    private val motionArtwork: MotionArtworkController,
) {
    private var lyricsJob: Job? = null
    private var preloadJob: Job? = null

    fun updateReplayGain(track: Track?) {
        val settings = settingsPreferences.getSettings()
        val targetGain = if (settings.isReplayGainEnabled) (track?.replayGainDb ?: 0.0f) else 0.0f
        headroomLimiter.setTargetGainDb(targetGain)
        headroomLimiter.setEnabled(settings.isHeadroomLimiterEnabled)
        playbackState.value = playbackState.value.copy(
            activeGainDb = targetGain,
            hasReplayGain = settings.isReplayGainEnabled && track?.replayGainDb != null,
            replayGainOrigin = if (settings.isReplayGainEnabled) track?.replayGainOrigin else null
        )
    }

    fun loadLyricsIfMissing(track: Track) {
        if (track.lyrics.isNotEmpty()) return
        if (!settingsPreferences.getSettings().isOnlineLyricsEnabled) return
        lyricsJob?.cancel()
        lyricsJob = scope.launch(Dispatchers.IO) {
            val fetchedLyrics = LrclibLyricsProvider.getLyrics(context, track)
            if (fetchedLyrics.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    val current = playbackState.value.currentTrack
                    if (current?.id == track.id) {
                        val updatedTrack = current.copy(lyrics = fetchedLyrics)
                        val updatedQueue = playbackState.value.queue.map {
                            if (it.id == track.id) updatedTrack else it
                        }
                        playbackState.value = playbackState.value.copy(
                            currentTrack = updatedTrack,
                            queue = updatedQueue
                        )
                    }
                }
            }
        }
    }

    fun preloadSurroundingTracks(queue: List<Track>, currentIndex: Int) {
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
            motionArtwork.prefetchNeighbors(nextTrack, prevTrack)
        }
    }

    fun cancelLyrics() {
        lyricsJob?.cancel()
    }

    fun cancelPreload() {
        preloadJob?.cancel()
    }
}
