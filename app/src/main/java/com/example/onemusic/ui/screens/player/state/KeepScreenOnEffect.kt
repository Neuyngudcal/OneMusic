package com.example.onemusic.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Giữ màn hình sáng khi đang mở Now Playing nếu bật trong Cài đặt (gỡ cờ khi đóng sheet). */
@Composable
internal fun KeepScreenOnEffect(isKeepScreenOnEnabled: Boolean?) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(isKeepScreenOnEnabled) {
        val shouldKeepOn = isKeepScreenOnEnabled == true
        if (shouldKeepOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            // Chỉ xóa cờ nếu chính sheet này đã bật, tránh tắt nhầm cờ do nơi khác đặt
            if (shouldKeepOn) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
