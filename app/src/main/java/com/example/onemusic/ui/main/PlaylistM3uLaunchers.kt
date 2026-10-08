package com.example.onemusic.ui.main

import androidx.compose.runtime.remember
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.playlist.M3uPlaylistManager
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.onemusic.ui.utils.ShowSnackbar
import androidx.compose.foundation.layout.size

/** Hai thao tác playlist .m3u8 dùng Storage Access Framework (người dùng chọn nơi lưu / tệp để nhập). */
internal class PlaylistM3uLaunchers(
    val exportPlaylist: (CustomPlaylist) -> Unit,
    val importPlaylist: () -> Unit
)

/** Đăng ký launcher xuất/nhập M3U8. Kết quả báo bằng Snackbar dùng chung. */
@Composable
internal fun rememberPlaylistM3uLaunchers(
    tracks: List<Track>,
    musicRepository: MusicRepository,
    showSnackbar: ShowSnackbar
): PlaylistM3uLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Playlist đang chờ người dùng chọn nơi lưu
    var playlistToExport by remember { mutableStateOf<CustomPlaylist?>(null) }

    // M3U8 Export Launcher (SAF CreateDocument)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null && playlistToExport != null) {
            val pl = playlistToExport ?: return@rememberLauncherForActivityResult
            scope.launch {
                val content = M3uPlaylistManager.exportPlaylistToM3uString(pl, tracks)
                val success = M3uPlaylistManager.writeContentToUri(context, uri, content)
                if (success) {
                    showSnackbar("Đã xuất playlist \"${pl.name}\"", null, null)
                } else {
                    showSnackbar("Không xuất được playlist. Hãy thử chọn vị trí lưu khác.", null, null)
                }
                playlistToExport = null
            }
        }
    }

    // M3U8 Import Launcher (SAF OpenDocument)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = M3uPlaylistManager.importPlaylistFromUri(context, uri, tracks)
                if (result != null) {
                    val (name, trackIds) = result
                    val newPl = musicRepository.createPlaylist(name)
                    if (newPl != null) {
                        for (tid in trackIds) {
                            musicRepository.addTrackToPlaylist(newPl.id, tid)
                        }
                        showSnackbar("Đã nhập playlist \"$name\" (${trackIds.size} bài hát)", null, null)
                    }
                } else {
                    showSnackbar("Không đọc được tệp playlist. Hãy chọn tệp .m3u hoặc .m3u8 khác.", null, null)
                }
            }
        }
    }

    return PlaylistM3uLaunchers(
        exportPlaylist = { pl ->
            playlistToExport = pl
            exportLauncher.launch("${pl.name}.m3u8")
        },
        importPlaylist = {
            importLauncher.launch(arrayOf("audio/*", "application/x-mpegurl", "audio/x-mpegurl", "text/plain", "*/*"))
        }
    )
}
