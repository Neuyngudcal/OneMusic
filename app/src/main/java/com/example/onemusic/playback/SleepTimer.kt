package com.example.onemusic.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hẹn giờ tắt nhạc: đếm ngược theo phút (cập nhật `sleepTimerRemainingSeconds` mỗi giây)
 * hoặc "hết bài này" (`sleepTimerMinutes = -1`, xử lý khi bài kết thúc trong controller).
 * Hết giờ thì gọi [onTimerFinished] (controller fade out rồi tạm dừng).
 */
internal class SleepTimer(
    private val scope: CoroutineScope,
    private val playbackState: MutableStateFlow<PlaybackState>,
    private val onTimerFinished: () -> Unit,
) {
    private var sleepTimerJob: Job? = null

    fun set(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            cancel()
            return
        }
        val totalSeconds = minutes * 60L
        playbackState.value = playbackState.value.copy(
            sleepTimerMinutes = minutes,
            sleepTimerRemainingSeconds = totalSeconds
        )
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                playbackState.value = playbackState.value.copy(
                    sleepTimerRemainingSeconds = remaining
                )
            }
            if (isActive) {
                onTimerFinished()
            }
        }
    }

    fun setEndOfTrack() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        playbackState.value = playbackState.value.copy(
            sleepTimerMinutes = -1,
            sleepTimerRemainingSeconds = null
        )
    }

    fun cancel() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        playbackState.value = playbackState.value.copy(
            sleepTimerMinutes = null,
            sleepTimerRemainingSeconds = null
        )
    }
}
