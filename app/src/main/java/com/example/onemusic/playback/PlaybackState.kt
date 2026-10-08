package com.example.onemusic.playback

import androidx.compose.runtime.Immutable
import com.example.onemusic.data.model.Track

enum class RepeatMode {
    OFF, ALL, ONE
}

@Immutable
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isAutoplay: Boolean = true,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val visualizerAmplitudes: List<Float> = List(16) { 0.2f },
    val activeGainDb: Float = 0.0f,
    val hasReplayGain: Boolean = false,
    val replayGainOrigin: String? = null,
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemainingSeconds: Long? = null,
    val transitionDirection: Int = 0 // 1 for Next (slide left), -1 for Prev (slide right), 0 for Direct
)
