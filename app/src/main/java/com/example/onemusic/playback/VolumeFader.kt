package com.example.onemusic.playback

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tăng/giảm âm lượng mượt (fade in / fade out) khi phát, tạm dừng, chuyển bài.
 * Giữ `fadeJob` – mỗi lúc chỉ có một lần fade chạy, lần mới hủy lần cũ.
 * Âm lượng người dùng (`userVolume`) do [MusicPlayerController] giữ, ở đây chỉ đọc qua [userVolume].
 */
internal class VolumeFader(
    private val scope: CoroutineScope,
    private val player: () -> ExoPlayer?,
    private val userVolume: () -> Float,
) {
    private var fadeJob: Job? = null

    /** true khi đang có một lần fade chạy (theo dõi vị trí dùng để không tự giảm âm lượng crossfade chồng lên). */
    val isFading: Boolean
        get() = fadeJob?.isActive == true

    fun cancel() {
        fadeJob?.cancel()
    }

    fun fadeIn(durationMs: Long = 150L) {
        val player = player() ?: return
        fadeJob?.cancel()
        player.volume = 0f
        player.play()
        fadeJob = scope.launch {
            val steps = 20
            val stepDelay = (durationMs / steps).coerceAtLeast(8L)
            for (i in 1..steps) {
                if (!isActive) break
                val factor = i.toFloat() / steps
                player.volume = factor * userVolume()
                delay(stepDelay)
            }
            if (isActive) {
                player.volume = userVolume()
            }
        }
    }

    fun fadeOut(durationMs: Long = 150L, onComplete: () -> Unit) {
        val player = player()
        if (player == null || !player.isPlaying) {
            onComplete()
            return
        }
        fadeJob?.cancel()
        val startVolume = player.volume
        fadeJob = scope.launch {
            val steps = 20
            val stepDelay = (durationMs / steps).coerceAtLeast(8L)
            for (i in steps downTo 0) {
                if (!isActive) break
                val factor = i.toFloat() / steps
                player.volume = factor * startVolume
                delay(stepDelay)
            }
            onComplete()
        }
    }
}
