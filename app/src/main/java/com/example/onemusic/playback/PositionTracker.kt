package com.example.onemusic.playback

import androidx.media3.exoplayer.ExoPlayer
import com.example.onemusic.data.local.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Vòng lặp theo dõi vị trí phát khi đang phát (40ms/lần, 250ms khi dừng):
 * - ghi vị trí vào [positionMs] (luồng riêng, không làm vẽ lại màn hình đọc playbackState);
 * - cập nhật độ dài bài khi thay đổi, tự lưu vị trí mỗi ~5 giây;
 * - tự giảm âm lượng gần cuối bài khi bật Crossfade.
 * Hai StateFlow do [MusicPlayerController] sở hữu; lớp này chỉ ghi vào.
 */
internal class PositionTracker(
    private val scope: CoroutineScope,
    private val player: () -> ExoPlayer?,
    private val playbackState: MutableStateFlow<PlaybackState>,
    private val positionMs: MutableStateFlow<Long>,
    private val settingsPreferences: SettingsPreferences,
    private val isFading: () -> Boolean,
    private val userVolume: () -> Float,
) {
    private var progressJob: Job? = null

    fun start() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var autoSaveTicks = 0
            while (isActive) {
                var isPlaying = false
                player()?.let { player ->
                    isPlaying = player.isPlaying
                    val pos = player.currentPosition
                    val duration = if (player.duration > 0) player.duration else playbackState.value.currentTrack?.durationMs ?: 0L

                    // Chỉ ghi vào luồng vị trí riêng; playbackState chỉ đổi khi độ dài bài thật sự đổi
                    positionMs.value = pos
                    if (playbackState.value.durationMs != duration) {
                        playbackState.value = playbackState.value.copy(durationMs = duration)
                    }

                    // Auto-save playback position every ~5 seconds when playing
                    if (isPlaying) {
                        autoSaveTicks++
                        if (autoSaveTicks >= 125) { // 125 * 40ms = 5000ms
                            autoSaveTicks = 0
                            playbackState.value.currentTrack?.let { current ->
                                settingsPreferences.saveLastPlaybackState(current.id, pos)
                            }
                        }
                    }

                    // Auto-crossfade volume down near the very end of track
                    val settings = settingsPreferences.getSettings()
                    if (settings.isCrossfadeEnabled && duration > 10000L && isPlaying && !isFading()) {
                        val crossfadeMs = (settings.crossfadeDurationSeconds * 1000L).coerceIn(1000L, 8000L)
                        val remainingMs = duration - pos
                        if (remainingMs in 0L..crossfadeMs) {
                            val factor = (remainingMs.toFloat() / crossfadeMs).coerceIn(0.05f, 1f)
                            player.volume = factor * userVolume()
                        }
                    }
                }
                if (isPlaying) {
                    delay(40)
                } else {
                    delay(250)
                }
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
    }
}
