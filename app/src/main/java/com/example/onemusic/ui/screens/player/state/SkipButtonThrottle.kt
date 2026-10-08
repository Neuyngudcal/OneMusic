package com.example.onemusic.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Bỏ qua lần bấm Trước/Sau cách lần trước chưa tới 350 ms (dùng chung một mốc cho cả hai nút). */
internal class SkipButtonThrottle {
    private var lastButtonSkipTimeMs by mutableLongStateOf(0L)
    private val buttonThrottleMs = 350L

    fun run(action: () -> Unit) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastButtonSkipTimeMs >= buttonThrottleMs) {
            lastButtonSkipTimeMs = now
            action()
        }
    }
}

@Composable
internal fun rememberSkipButtonThrottle(): SkipButtonThrottle = remember { SkipButtonThrottle() }
