package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.utils.rememberScanWithPermission
import android.content.Intent
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.ArtistImageRepository
import java.util.Calendar
import dev.chrisbanes.haze.HazeState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.theme.LocalApexHazeState

/**
 * Home Screen: Displays library category navigation (Playlist, Nghệ sĩ, Album, Bài hát)
 * with authentic Samsung One UI 8.5 spacious collapsible large viewing area (media_1787290661517.jpg).
 * - Upper ~40% is the expansive Viewing Area with large title "Trang Chủ", generous breathable empty space,
 *   and 3-dots menu button sitting at the bottom-right of the viewing space right above the categories.
 * - When scrolling down: Collapses smoothly, with pinned floating capsule at top.
 */
@Composable
fun HomeScreen(
    tracks: List<Track>,
    currentTrackId: String?,
    customPlaylists: List<CustomPlaylist> = emptyList(),
    // track = bài được bấm, context = danh sách đang hiển thị (dùng làm hàng đợi)
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreatePlaylist: (String) -> CustomPlaylist? = { null },
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    // (playlistId, trackId) – dùng cho "Hoàn tác" khi vừa bỏ bài khỏi playlist
    onAddTrackToPlaylist: ((String, String) -> Unit)? = null,
    onPlayTracks: (List<Track>) -> Unit = {},
    onPlayNext: ((Track) -> Unit)? = null,
    // Menu thao tác từng bài: mở dialog thêm 1 bài vào playlist / thêm vào cuối hàng đợi
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onExportPlaylistM3u: ((CustomPlaylist) -> Unit)? = null,
    onImportPlaylistM3u: (() -> Unit)? = null,
    onOpenFolders: () -> Unit = {},
    onRescan: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showSnackbar = com.example.onemusic.ui.utils.LocalAppSnackbar.current
    val listState = rememberLazyListState()
    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }
    var currentSubView by rememberSaveable { mutableStateOf(HomeSubView.MAIN) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    // Playlist đang chờ người dùng xác nhận xóa (null = không có hộp xác nhận)
    var playlistPendingDelete by remember { mutableStateOf<CustomPlaylist?>(null) }
    val settingsPreferences = remember { SettingsPreferences(context) }
    val appSettings by settingsPreferences.settingsFlow.collectAsState()
    val artistImageRepository = remember { ArtistImageRepository.getInstance(context) }
    val artistImages by artistImageRepository.artistImagesFlow.collectAsState()

    var selectedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    // Khóa album (LibraryGrouping.albumKey), không phải tên – 2 album khác nhau có thể trùng tên
    var selectedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
    // Lưu id (rememberSaveable không lưu được object CustomPlaylist), tra lại từ customPlaylists khi cần
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var isSelectedFavorites by rememberSaveable { mutableStateOf(false) }
    var albumViewMode by rememberSaveable { mutableStateOf(AlbumViewMode.GRID_2) }
    var isAlbumViewMenuExpanded by remember { mutableStateOf(false) }
    var artistGroupMode by rememberSaveable { mutableStateOf(ArtistGroupMode.MERGED) }
    var isArtistGroupMenuExpanded by remember { mutableStateOf(false) }
    // Bài đang mở menu thao tác (nhấn giữ) – null = đóng
    var trackForActions by remember { mutableStateOf<Track?>(null) }

    // Gộp nghệ sĩ bằng hàm dùng chung với Thư viện & Tìm kiếm
    val artists = remember(tracks) { LibraryGrouping.groupArtists(tracks).map { it.first } }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingTitle = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Chào buổi sáng"
            in 12..17 -> "Chào buổi chiều"
            else -> "Chào buổi tối"
        }
    }
    val greetingSubtitle = remember(tracks.size) {
        if (tracks.isNotEmpty()) "${tracks.size} bài hát sẵn sàng" else "Không gian âm nhạc cá nhân"
    }

    // Danh sách id: so sánh bằng nội dung → bấm tim (tạo list `tracks` mới, cùng id) không làm nó đổi.
    // Chỉ khi thêm/bớt bài (quét lại) thì banner & gợi ý mới được chọn ngẫu nhiên lại.
    val trackIds = remember(tracks) { tracks.map { it.id } }
    val trackById = remember(tracks) { tracks.associateBy { it.id } }

    // Không fallback về tracks.take(8): chưa nghe gì thì hiện thẻ "Chưa có bài hát nào được phát gần đây"
    val recentlyPlayedTracks = remember(trackById, appSettings.recentlyPlayedTrackIds) {
        appSettings.recentlyPlayedTrackIds.mapNotNull { trackById[it] }.take(8)
    }

    val topArtists = remember(artists, tracks) {
        artists.map { artistName ->
            val count = tracks.count { it.artist.contains(artistName, ignoreCase = true) }
            artistName to count
        }.sortedByDescending { it.second }.take(8)
    }

    val favoriteTracks = remember(tracks) { tracks.filter { it.isFavorite } }

    // Chỉ lưu id bài được chọn; tra lại object mới nhất từ trackById để trạng thái tim luôn đúng
    val featuredTrackId = remember(trackIds) {
        val withArtwork = tracks.filter { it.artworkUrl.isNotBlank() }
        (withArtwork.filter { it.isFavorite }.randomOrNull()
            ?: withArtwork.randomOrNull()
            ?: tracks.randomOrNull())?.id
    }
    val featuredTrack = featuredTrackId?.let { trackById[it] }

    // Không phụ thuộc "Nghe gần đây" → nghe xong một bài thì gợi ý không bị xáo lại trước mắt
    val suggestedTrackIds = remember(trackIds) {
        val recentIds = appSettings.recentlyPlayedTrackIds.toSet()
        tracks.filter { it.id !in recentIds && it.artworkUrl.isNotBlank() }
            .shuffled()
            .take(8)
            .map { it.id }
    }
    val suggestedTracks = remember(suggestedTrackIds, trackById) {
        suggestedTrackIds.mapNotNull { trackById[it] }
    }

    LaunchedEffect(artists, appSettings.isAutoDownloadArtistImagesEnabled) {
        if (appSettings.isAutoDownloadArtistImagesEnabled) {
            artistImageRepository.prefetchArtists(artists, true)
        }
    }

    fun shareApp() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Trải nghiệm OneMusic - Trình phát nhạc Samsung One UI 8.5 chất lượng cao, hỗ trợ Hi-Res Lossless & ReplayGain!"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ OneMusic")
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            showSnackbar("Không mở được trình chia sẻ. Hãy thử lại sau.", null, null)
        }
    }

    // Xin quyền đọc nhạc (nếu chưa có) rồi quét – logic dùng chung ở ui/utils/ScanWithPermission.kt
    val triggerScanWithPermission = rememberScanWithPermission(onRescan)

    // Màn đang đứng trước khi mở trang chi tiết → nút ← / Back quay về đúng chỗ đó
    // (vd mở nghệ sĩ từ carousel Trang chủ thì quay về Trang chủ, không phải danh sách nghệ sĩ)
    var detailBackTarget by rememberSaveable { mutableStateOf(HomeSubView.MAIN) }
    fun openDetail(view: HomeSubView) {
        detailBackTarget = currentSubView
        currentSubView = view
    }

    // Intercept back gesture if a menu is open or inside a subview/detail view
    BackHandler(
        enabled = isAlbumViewMenuExpanded || isArtistGroupMenuExpanded || currentSubView != HomeSubView.MAIN
    ) {
        when {
            isAlbumViewMenuExpanded -> isAlbumViewMenuExpanded = false
            isArtistGroupMenuExpanded -> isArtistGroupMenuExpanded = false
            currentSubView in setOf(HomeSubView.ARTIST_DETAIL, HomeSubView.ALBUM_DETAIL, HomeSubView.PLAYLIST_DETAIL) ->
                currentSubView = detailBackTarget
            else -> currentSubView = HomeSubView.MAIN
        }
    }



    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        AnimatedContent(
            targetState = currentSubView,
            transitionSpec = {
                val isForward = targetState != HomeSubView.MAIN
                if (isForward) {
                    (slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                    ) + fadeIn(animationSpec = spring(stiffness = 500f))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                        ) + fadeOut(animationSpec = spring(stiffness = 500f))
                    )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                    ) + fadeIn(animationSpec = spring(stiffness = 500f))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f)
                        ) + fadeOut(animationSpec = spring(stiffness = 500f))
                    )
                }
            },
            label = "home_view_transition"
        ) { subView ->
            when (subView) {
                HomeSubView.MAIN -> {
                    HomeMainContent(
                        listState = listState,
                        tracks = tracks,
                        currentTrackId = currentTrackId,
                        greetingTitle = greetingTitle,
                        greetingSubtitle = greetingSubtitle,
                        featuredTrack = featuredTrack,
                        recentlyPlayedTracks = recentlyPlayedTracks,
                        suggestedTracks = suggestedTracks,
                        customPlaylists = customPlaylists,
                        favoriteTrackCount = favoriteTracks.size,
                        topArtists = topArtists,
                        artistImages = artistImages,
                        artistImageRepository = artistImageRepository,
                        onTrackSelect = onTrackSelect,
                        onTrackLongClick = { trackForActions = it },
                        onScanLibrary = { triggerScanWithPermission() },
                        onOpenFolders = onOpenFolders,
                        onNavigate = { currentSubView = it },
                        onOpenFavorites = {
                            isSelectedFavorites = true
                            selectedPlaylistId = null
                            selectedAlbum = null
                            selectedArtist = null
                            openDetail(HomeSubView.PLAYLIST_DETAIL)
                        },
                        onOpenPlaylist = { pl ->
                            selectedPlaylistId = pl.id
                            isSelectedFavorites = false
                            selectedAlbum = null
                            selectedArtist = null
                            openDetail(HomeSubView.PLAYLIST_DETAIL)
                        },
                        onOpenArtist = { artistName ->
                            selectedArtist = artistName
                            isSelectedFavorites = false
                            selectedPlaylistId = null
                            selectedAlbum = null
                            openDetail(HomeSubView.ARTIST_DETAIL)
                        }
                    )
                }

                HomeSubView.PLAYLISTS -> {
                    HomePlaylistsContent(
                        tracks = tracks,
                        customPlaylists = customPlaylists,
                        onBack = { currentSubView = HomeSubView.MAIN },
                        onImportPlaylistM3u = onImportPlaylistM3u,
                        onExportPlaylistM3u = onExportPlaylistM3u,
                        onCreatePlaylistClick = { showNewPlaylistDialog = true },
                        onRequestDeletePlaylist = { playlistPendingDelete = it },
                        onOpenFavorites = {
                            isSelectedFavorites = true
                            selectedPlaylistId = null
                            openDetail(HomeSubView.PLAYLIST_DETAIL)
                        },
                        onOpenPlaylist = { pl ->
                            selectedPlaylistId = pl.id
                            isSelectedFavorites = false
                            openDetail(HomeSubView.PLAYLIST_DETAIL)
                        }
                    )
                }

                HomeSubView.ARTISTS -> {
                    HomeArtistsContent(
                        tracks = tracks,
                        artistGroupMode = artistGroupMode,
                        onArtistGroupModeChange = { artistGroupMode = it },
                        isGroupMenuExpanded = isArtistGroupMenuExpanded,
                        onGroupMenuExpandedChange = { isArtistGroupMenuExpanded = it },
                        hazeState = hazeState,
                        artistImages = artistImages,
                        artistImageRepository = artistImageRepository,
                        onBack = { currentSubView = HomeSubView.MAIN },
                        onOpenArtist = { name ->
                            selectedArtist = name
                            openDetail(HomeSubView.ARTIST_DETAIL)
                        }
                    )
                }

                HomeSubView.ALBUMS -> {
                    HomeAlbumsContent(
                        tracks = tracks,
                        albumViewMode = albumViewMode,
                        onAlbumViewModeChange = { albumViewMode = it },
                        isViewMenuExpanded = isAlbumViewMenuExpanded,
                        onViewMenuExpandedChange = { isAlbumViewMenuExpanded = it },
                        hazeState = hazeState,
                        onBack = { currentSubView = HomeSubView.MAIN },
                        onOpenAlbum = { key ->
                            selectedAlbum = key
                            openDetail(HomeSubView.ALBUM_DETAIL)
                        },
                        onPlayTracks = onPlayTracks
                    )
                }

                HomeSubView.ARTIST_DETAIL -> {
                    HomeArtistDetailRoute(
                        selectedArtist = selectedArtist,
                        tracks = tracks,
                        artistGroupMode = artistGroupMode,
                        artistImages = artistImages,
                        artistImageRepository = artistImageRepository,
                        currentTrackId = currentTrackId,
                        onTrackSelect = onTrackSelect,
                        onPlayTracks = onPlayTracks,
                        onToggleFavorite = onToggleFavorite,
                        onOpenAddToPlaylist = onOpenAddToPlaylist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onBack = { currentSubView = detailBackTarget }
                    )
                }

                HomeSubView.ALBUM_DETAIL -> {
                    HomeAlbumDetailRoute(
                        selectedAlbum = selectedAlbum,
                        tracks = tracks,
                        currentTrackId = currentTrackId,
                        onTrackSelect = onTrackSelect,
                        onPlayTracks = onPlayTracks,
                        onToggleFavorite = onToggleFavorite,
                        onOpenAddToPlaylist = onOpenAddToPlaylist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onBack = { currentSubView = detailBackTarget }
                    )
                }

                HomeSubView.PLAYLIST_DETAIL -> {
                    HomePlaylistDetailRoute(
                        isSelectedFavorites = isSelectedFavorites,
                        selectedPlaylistId = selectedPlaylistId,
                        customPlaylists = customPlaylists,
                        tracks = tracks,
                        currentTrackId = currentTrackId,
                        onTrackSelect = onTrackSelect,
                        onPlayTracks = onPlayTracks,
                        onToggleFavorite = onToggleFavorite,
                        onOpenAddToPlaylist = onOpenAddToPlaylist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                        onAddTrackToPlaylist = onAddTrackToPlaylist,
                        onRenamePlaylist = onRenamePlaylist,
                        onDeletePlaylist = onDeletePlaylist,
                        onBack = { currentSubView = detailBackTarget }
                    )
                }
            }
        }

        // New Playlist Dialog
        if (showNewPlaylistDialog) {
            NewPlaylistDialog(
                name = newPlaylistName,
                onNameChange = { newPlaylistName = it },
                hazeState = hazeState,
                onCreatePlaylist = { onCreatePlaylist(it) },
                onDismiss = { showNewPlaylistDialog = false }
            )
        }

        // Xác nhận trước khi xóa playlist (khó khôi phục)
        playlistPendingDelete?.let { pl ->
            ApexConfirmDialog(
                title = "Xóa danh sách phát?",
                message = "Playlist \"${pl.name}\" sẽ bị xóa. Các bài hát gốc trên máy không bị ảnh hưởng.",
                confirmButtonText = "Xóa",
                isDestructive = true,
                onConfirm = {
                    onDeletePlaylist(pl.id)
                    playlistPendingDelete = null
                },
                onDismiss = { playlistPendingDelete = null }
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

