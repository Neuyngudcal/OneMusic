package com.example.onemusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.onemusic.data.model.Track

/**
 * Menu thao tác cho MỘT bài ở ngoài Now Playing (Home, Thư viện, Tìm kiếm, trang chi tiết).
 * Gói ApexTrackActionSheet + dialog "Thông tin bài hát" (thông số kỹ thuật) để các màn không phải lặp code.
 *
 * Cố ý KHÔNG có "Xóa khỏi hàng đợi" và "Hẹn giờ tắt" – hai mục đó chỉ có nghĩa trong Now Playing.
 *
 * @param track bài đang mở menu; null = menu đóng.
 */
@Composable
fun TrackActionMenu(
    track: Track?,
    onDismiss: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null
) {
    var trackForDetails by remember { mutableStateOf<Track?>(null) }

    track?.let { t ->
        ApexTrackActionSheet(
            track = t,
            onDismissRequest = onDismiss,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onOpenCredits = { trackForDetails = t }
        )
    }

    trackForDetails?.let { t ->
        TrackDetailsDialog(
            track = t,
            onDismiss = { trackForDetails = null },
            onPlayNext = onPlayNext
        )
    }
}
