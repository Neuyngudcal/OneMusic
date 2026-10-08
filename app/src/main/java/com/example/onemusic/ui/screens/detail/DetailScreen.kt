package com.example.onemusic.ui.screens.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Clean & Immersive Hero Detail Screen for Album, Artist, and Playlist.
 * Conforms 100% to Samsung One UI & Apex Prism AMOLED Obsidian Design Specifications.
 */
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    currentTrackId: String?,
    // track = bài được bấm, context = danh sách đang hiển thị (dùng làm hàng đợi)
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    isArtist: Boolean = false,
    isFavorites: Boolean = false,
    customPlaylist: CustomPlaylist? = null,
    onRemoveTrackFromPlaylist: ((playlistId: String, trackId: String) -> Unit)? = null,
    // Dùng cho "Hoàn tác" khi vừa bỏ một bài khỏi playlist
    onAddTrackToPlaylist: ((playlistId: String, trackId: String) -> Unit)? = null,
    onRenamePlaylist: ((playlistId: String, newName: String) -> Unit)? = null,
    onDeletePlaylist: ((playlistId: String) -> Unit)? = null,
    // Menu thao tác từng bài (nhấn giữ một bài)
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val showSnackbar = com.example.onemusic.ui.utils.LocalAppSnackbar.current
    val settingsPreferences = remember { SettingsPreferences(context) }
    val appSettings by settingsPreferences.settingsFlow.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(customPlaylist?.name ?: "") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    // Bài đang mở menu thao tác (nhấn giữ) – null = đóng
    var trackForActions by remember { mutableStateOf<Track?>(null) }

    val listState = rememberLazyListState()
    // Đã cuộn qua phần ảnh bìa + tên (item đầu) → hiện tên trên thanh trên cùng, kèm nền tối
    val showTopTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val topBarBackgroundAlpha by animateFloatAsState(
        targetValue = if (showTopTitle) 0.85f else 0f,
        label = "detail_top_bar_bg"
    )
    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // LAYER 1: HERO BACKGROUND LAYER (520dp - Góc nhìn điện ảnh tràn viền)
        DetailHeroBackground(
            title = title,
            artworkUrl = artworkUrl,
            isArtist = isArtist,
            isFavorites = isFavorites,
            listState = listState
        )

        // LAYER 2: FOREGROUND SCROLL LAYER (LazyColumn trượt êm đè lên ảnh)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState, key = "detail_screen_scroll"),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. Khoảng trống để lộ ảnh bìa sáng rõ ở trên (300dp)
            item(key = "hero_album_header") {
                DetailHeroHeader(
                    title = title,
                    subtitle = subtitle,
                    tracks = tracks,
                    onPlayAll = onPlayAll,
                    onShuffleAll = onShuffleAll,
                    showSnackbar = showSnackbar
                )
            }

            // 2. Danh Sách Bài Hát Grouped Cards (Nền đen ObsidianBlack bao phủ toàn diện)
            val total = tracks.size
            itemsIndexed(
                items = tracks,
                key = { index, track -> "${track.id}_$index" },
                contentType = { _, _ -> "track_item" }
            ) { index, track ->
                val isCurrent = track.id == currentTrackId
                DetailTrackRow(
                    index = index,
                    total = total,
                    track = track,
                    tracks = tracks,
                    isCurrent = isCurrent,
                    isHiResBadgeEnabled = appSettings.isHiResBadgeEnabled,
                    hazeState = hazeState,
                    customPlaylist = customPlaylist,
                    onTrackSelect = onTrackSelect,
                    onTrackLongClick = { trackForActions = track },
                    onToggleFavorite = onToggleFavorite,
                    onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                    onAddTrackToPlaylist = onAddTrackToPlaylist,
                    showSnackbar = showSnackbar
                )
            }
        }

        // LAYER 3: FLOATING STICKY TOP BAR (Trong suốt khi ở đầu trang; cuộn qua ảnh bìa thì hiện nền tối + tên)
        DetailStickyTopBar(
            title = title,
            subtitle = subtitle,
            tracks = tracks,
            showTopTitle = showTopTitle,
            topBarBackgroundAlpha = { topBarBackgroundAlpha },
            hazeState = hazeState,
            customPlaylist = customPlaylist,
            onBack = onBack,
            onRenameClick = { playlist ->
                renameInput = playlist.name
                showRenameDialog = true
            },
            onDeleteClick = { showDeleteConfirmDialog = true }
        )

        // LAYER 4: DIALOGS
        if (showRenameDialog && customPlaylist != null && onRenamePlaylist != null) {
            RenamePlaylistDialog(
                renameInput = renameInput,
                onRenameInputChange = { renameInput = it },
                // Tên mới hiện ngay trên màn → không cần thông báo
                onRename = { newName -> onRenamePlaylist(customPlaylist.id, newName) },
                onDismiss = { showRenameDialog = false }
            )
        }

        if (showDeleteConfirmDialog && customPlaylist != null && onDeletePlaylist != null) {
            ApexConfirmDialog(
                title = "Xóa danh sách phát?",
                message = "Bạn có chắc chắn muốn xóa playlist \"${customPlaylist.name}\"? Các bài hát gốc trên máy sẽ không bị ảnh hưởng.",
                confirmButtonText = "Xóa",
                dismissButtonText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    onDeletePlaylist(customPlaylist.id)
                    showSnackbar("Đã xóa playlist \"${customPlaylist.name}\"", null, null)
                    showDeleteConfirmDialog = false
                    onBack()
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }

        // Menu thao tác của bài vừa nhấn giữ (Phát kế tiếp, Thêm vào playlist, Thông tin bài hát...)
        com.example.onemusic.ui.components.TrackActionMenu(
            track = trackForActions,
            onDismiss = { trackForActions = null },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue
        )
    }
}
