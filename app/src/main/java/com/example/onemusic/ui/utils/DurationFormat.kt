package com.example.onemusic.ui.utils

import com.example.onemusic.data.model.Track
import java.util.Locale

/**
 * Định dạng thời lượng dùng chung cho mọi màn hình.
 *
 * - `padMinutes = false` (Now Playing, hàng đợi): 65_000 → "1:05"
 * - `padMinutes = true`  (Thư viện, Dọn trùng lặp, hẹn giờ tắt): 65_000 → "01:05"
 *
 * Giá trị âm được coi là 0. Dùng Locale.US để chữ số luôn là 0-9 bất kể ngôn ngữ máy.
 */
fun formatDuration(ms: Long, padMinutes: Boolean = false): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val pattern = if (padMinutes) "%02d:%02d" else "%d:%02d"
    return String.format(Locale.US, pattern, totalSeconds / 60, totalSeconds % 60)
}

/** Thời gian còn lại của bài, dạng "-3:12". Chưa biết độ dài bài → "--:--". */
fun formatRemaining(currentMs: Long, totalMs: Long): String {
    if (totalMs <= 0) return "--:--"
    val remainingMs = (totalMs - currentMs).coerceAtLeast(0)
    return "-" + formatDuration(remainingMs)
}

/** Tổng thời lượng danh sách bài, dạng "42 phút" hoặc "1 giờ 5 phút". */
fun formatTotalDuration(tracks: List<Track>): String {
    val totalMs = tracks.sumOf { it.durationMs }
    val totalMinutes = totalMs / 60000
    return if (totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        "$hours giờ $mins phút"
    } else {
        "$totalMinutes phút"
    }
}
