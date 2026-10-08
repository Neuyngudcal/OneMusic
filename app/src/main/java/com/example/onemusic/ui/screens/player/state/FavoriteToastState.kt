package com.example.onemusic.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Thông báo nổi "Đã ưa thích" / "Đã xóa khỏi phần ưa thích" trên Now Playing – tự ẩn sau 2,5 giây. */
@Stable
internal class FavoriteToastState(private val scope: CoroutineScope) {
    var message by mutableStateOf<String?>(null)
        private set

    private var job by mutableStateOf<Job?>(null)

    fun show(message: String) {
        this.message = message
        // Hủy hẹn giờ của lần trước để thông báo mới hiện đủ thời gian
        job?.cancel()
        job = scope.launch {
            delay(2500L)
            this@FavoriteToastState.message = null
        }
    }
}

@Composable
internal fun rememberFavoriteToastState(scope: CoroutineScope): FavoriteToastState =
    remember(scope) { FavoriteToastState(scope) }
