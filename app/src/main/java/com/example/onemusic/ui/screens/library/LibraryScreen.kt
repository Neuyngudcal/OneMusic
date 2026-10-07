package com.example.onemusic.ui.screens.library

import androidx.compose.material3.minimumInteractiveComponentSize

import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.screens.detail.DetailScreen
import com.example.onemusic.ui.utils.rememberScanWithPermission
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.onemusic.R
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.example.onemusic.haptics.rememberApexHaptics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.data.search.ArtistExtractor
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.components.AddToPlaylistMultipleDialog
import com.example.onemusic.ui.components.ApexAlphabetScroller
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.components.ApexDropdownDivider
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.ShadowColor
import com.example.onemusic.theme.SquircleLarge
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.theme.avatarColorFor

/**
 * Số item header nằm TRƯỚC bài đầu tiên ở tab Bài hát: library_header, library_main_filter_tabs,
 * filter_tabs_row, quick_actions_row, songs_section_label. Thêm/bớt header thì nhớ sửa hằng số này.
 */
private const val SONGS_HEADER_ITEM_COUNT = 5

/**
 * Main management filter tabs in LibraryScreen.
 */
enum class LibraryTab(val title: String) {
    SONGS("Tất cả bài hát"),
    ALBUMS("Album"),
    ARTISTS("Nghệ sĩ")
}

/**
 * Filter and sort options for the song list in LibraryScreen.
 */
enum class SongSortOption(val title: String) {
    ALL("Tất cả"),
    TITLE_AZ("Tên A-Z"),
    ARTIST("Nghệ sĩ"),
    DURATION("Thời lượng")
}

/**
 * View mode options for the song list in LibraryScreen.
 */
enum class LibraryViewMode(val title: String) {
    LIST("Danh sách"),
    GRID_2("Lưới 2 cột"),
    GRID_3("Lưới 3 cột")
}

/**
 * Main Library Screen: Clean list of all songs with Samsung One UI 8.5 aesthetics.
 * - Expansive collapsible viewing header ("Thư viện") with top-right MoreVert ⋮ menu.
 * - View mode switcher: Danh sách (LIST), Lưới 2 cột (GRID_2), Lưới 3 cột (GRID_3).
 * - One UI 8.5 Filter Tabs bar (Tất cả, Tên A-Z, Nghệ sĩ, Thời lượng) with borderless pill design.
 * - Real audio scanning integration (MediaStore & Custom folders) with permission handling.
 * - Grouped Card Container (`SurfaceCard` `#1B1C20`, `SquircleLarge` 26dp) with `SurfaceDivider`.
 * - Multi-Select (Batch Mode), Alphabet Fast Scroller, Track Audio Inspector Dialog.
 */
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    currentTrackId: String?,
    customPlaylists: List<CustomPlaylist> = emptyList(),
    onCreatePlaylist: (String) -> CustomPlaylist? = { null },
    onAddToPlaylist: (playlistId: String, trackId: String) -> Boolean = { _, _ -> false },
    onPlayTracks: ((List<Track>) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onPlayNextTracks: ((List<Track>) -> Unit)? = null,
    // Menu thao tác từng bài: mở dialog thêm 1 bài vào playlist / thêm vào cuối hàng đợi
    onOpenAddToPlaylist: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onImportPlaylistM3u: (() -> Unit)? = null,
    // track = bài được bấm, context = danh sách đang hiển thị (dùng làm hàng đợi)
    onTrackSelect: (track: Track, context: List<Track>) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRescan: () -> Unit = {},
    onOpenFolders: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val appSettings by settingsPreferences.settingsFlow.collectAsState()
    val artistImageRepository = remember { ArtistImageRepository.getInstance(context) }
    val artistImages by artistImageRepository.artistImagesFlow.collectAsState()

    // Bài đang mở menu thao tác (nút ⋮ ở cuối hàng) – null = đóng
    var trackForActions by remember { mutableStateOf<Track?>(null) }
    var currentLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    var currentSortOption by rememberSaveable { mutableStateOf(SongSortOption.ALL) }
    var sortAscending by rememberSaveable { mutableStateOf(true) }

    // Trang chi tiết album/nghệ sĩ đang mở (null = đang ở danh sách)
    // openedAlbum = khóa album (LibraryGrouping.albumKey), không phải tên – 2 album có thể trùng tên
    var openedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
    var openedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = openedAlbum != null || openedArtist != null) {
        openedAlbum = null
        openedArtist = null
    }

    // Gộp album/nghệ sĩ bằng hàm dùng chung với Trang chủ & Tìm kiếm → số lượng giống nhau ở mọi màn
    val albums = remember(tracks) { LibraryGrouping.groupAlbums(tracks) }

    val artists = remember(tracks) { LibraryGrouping.groupArtists(tracks) }

    val initialViewMode = remember(appSettings.libraryViewMode) {
        try {
            LibraryViewMode.valueOf(appSettings.libraryViewMode)
        } catch (_: Exception) {
            LibraryViewMode.LIST
        }
    }
    var libraryViewMode by remember { mutableStateOf(initialViewMode) }

    // Multi-Select (Batch Mode) states
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchAddToPlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedTrackIds = emptySet()
    }

    // Xin quyền đọc nhạc (nếu chưa có) rồi quét – logic dùng chung ở ui/utils/ScanWithPermission.kt
    val triggerScanWithPermission = rememberScanWithPermission(onRescan)

    // Dynamic sorting computation
    val sortedTracks = remember(tracks, currentSortOption, sortAscending) {
        when (currentSortOption) {
            SongSortOption.ALL -> tracks
            SongSortOption.TITLE_AZ -> {
                if (sortAscending) {
                    tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                } else {
                    tracks.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
                }
            }
            SongSortOption.ARTIST -> {
                if (sortAscending) {
                    tracks.sortedWith(
                        compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                    )
                } else {
                    tracks.sortedWith(
                        compareByDescending<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                    )
                }
            }
            SongSortOption.DURATION -> {
                if (sortAscending) {
                    tracks.sortedBy { it.durationMs }
                } else {
                    tracks.sortedByDescending { it.durationMs }
                }
            }
        }
    }

    val availableLetters = remember(sortedTracks) {
        sortedTracks.mapNotNull { it.title.trim().firstOrNull()?.uppercaseChar() }
            .map { if (it.isLetter()) it else '#' }
            .toSet()
    }

    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }
    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current

    fun findTrackIndexAtY(y: Float): Int {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val hitItem = visibleItems.firstOrNull { item ->
            y >= item.offset && y <= (item.offset + item.size)
        } ?: return -1
        // Khóa hàng có dạng "${track.id}_$index" → phần sau dấu '_' cuối là chỉ số trong sortedTracks.
        // Các item header ("library_header", "quick_actions_row"...) có phần cuối không phải số → toIntOrNull() = null.
        val key = hitItem.key as? String ?: return -1
        val index = key.substringAfterLast('_').toIntOrNull() ?: return -1
        return if (index in sortedTracks.indices) index else -1
    }

    val currentHeaderSubtitle = remember(currentLibraryTab, tracks.size, albums.size, artists.size) {
        when (currentLibraryTab) {
            LibraryTab.SONGS -> "${tracks.size} bài hát"
            LibraryTab.ALBUMS -> "${albums.size} album"
            LibraryTab.ARTISTS -> "${artists.size} nghệ sĩ"
        }
    }

    // Mục 7a: bấm album/nghệ sĩ → mở trang chi tiết (giống Home & Tìm kiếm), không phát ngay
    val playList: (List<Track>) -> Unit = { list ->
        if (list.isNotEmpty()) onPlayTracks?.invoke(list) ?: onTrackSelect(list.first(), list)
    }

    openedAlbum?.let { albumKey ->
        val album = albums.firstOrNull { it.key == albumKey }
        val albumTracks = album?.tracks.orEmpty()
        DetailScreen(
            title = album?.name ?: "Album",
            subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
            artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
            tracks = albumTracks,
            currentTrackId = currentTrackId,
            onTrackSelect = onTrackSelect,
            onPlayAll = playList,
            onShuffleAll = { playList(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { openedAlbum = null }
        )
        return
    }

    openedArtist?.let { artistName ->
        val artistTracks = artists.firstOrNull { it.first == artistName }?.second.orEmpty()
        DetailScreen(
            title = artistName,
            subtitle = "Nghệ sĩ",
            artworkUrl = artistImages[artistName.trim().lowercase()]
                ?: artistImageRepository.getCachedImageUrl(artistName),
            tracks = artistTracks,
            currentTrackId = currentTrackId,
            isArtist = true,
            onTrackSelect = onTrackSelect,
            onPlayAll = playList,
            onShuffleAll = { playList(it.shuffled()) },
            onToggleFavorite = onToggleFavorite,
            onOpenAddToPlaylist = onOpenAddToPlaylist,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onBack = { openedArtist = null }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. Top Header: Minimalist Clean Header with Compact Status Bar Padding
            item(key = "library_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Thư viện",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIvory,
                                fontSize = 32.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentHeaderSubtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    // Top-Right Action Menu (⋮) with Integrated View Mode Switcher
                    var isTopMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "Tùy chọn thư viện",
                            onClick = { isTopMenuOpen = true },
                            size = 40.dp,
                            iconSize = 22.dp
                        )

                        ApexDropdownMenu(
                            expanded = isTopMenuOpen,
                            onDismissRequest = { isTopMenuOpen = false },
                            hazeState = hazeState,
                            width = 250.dp
                        ) {
                            // Section: Chế độ hiển thị (Segmented Instant Switcher)
                            Text(
                                text = "CHẾ ĐỘ HIỂN THỊ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceCard)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Danh sách (LIST)
                                val isList = libraryViewMode == LibraryViewMode.LIST
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isList) PrimaryIvory else Color.Transparent)
                                        .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                            libraryViewMode = LibraryViewMode.LIST
                                            settingsPreferences.updateSettings { it.copy(libraryViewMode = LibraryViewMode.LIST.name) }
                                            isTopMenuOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                            contentDescription = "Danh sách",
                                            tint = if (isList) CharcoalBlack else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Danh sách",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isList) CharcoalBlack else TextSecondary,
                                                fontSize = 10.5.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 2. Lưới 2 cột (GRID_2)
                                val isGrid2 = libraryViewMode == LibraryViewMode.GRID_2
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isGrid2) PrimaryIvory else Color.Transparent)
                                        .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                            libraryViewMode = LibraryViewMode.GRID_2
                                            settingsPreferences.updateSettings { it.copy(libraryViewMode = LibraryViewMode.GRID_2.name) }
                                            isTopMenuOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GridView,
                                            contentDescription = "Lưới 2",
                                            tint = if (isGrid2) CharcoalBlack else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Lưới 2",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isGrid2) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isGrid2) CharcoalBlack else TextSecondary,
                                                fontSize = 10.5.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 3. Lưới 3 cột (GRID_3)
                                val isGrid3 = libraryViewMode == LibraryViewMode.GRID_3
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isGrid3) PrimaryIvory else Color.Transparent)
                                        .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                            libraryViewMode = LibraryViewMode.GRID_3
                                            settingsPreferences.updateSettings { it.copy(libraryViewMode = LibraryViewMode.GRID_3.name) }
                                            isTopMenuOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ViewModule,
                                            contentDescription = "Lưới 3",
                                            tint = if (isGrid3) CharcoalBlack else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Lưới 3",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isGrid3) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isGrid3) CharcoalBlack else TextSecondary,
                                                fontSize = 10.5.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            ApexDropdownDivider()

                            // Section: Actions
                            ApexDropdownMenuItem(
                                text = if (isMultiSelectMode) "Thoát chọn nhiều" else "Chọn nhiều bài hát",
                                icon = Icons.Rounded.SelectAll,
                                onClick = {
                                    isTopMenuOpen = false
                                    isMultiSelectMode = !isMultiSelectMode
                                    if (!isMultiSelectMode) selectedTrackIds = emptySet()
                                }
                            )
                            ApexDropdownDivider()
                            ApexDropdownMenuItem(
                                text = "Quét lại bài hát",
                                icon = Icons.Rounded.Refresh,
                                onClick = {
                                    isTopMenuOpen = false
                                    triggerScanWithPermission()
                                }
                            )
                            if (onImportPlaylistM3u != null) {
                                ApexDropdownMenuItem(
                                    text = "Nhập danh sách phát (.m3u8)",
                                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    onClick = {
                                        isTopMenuOpen = false
                                        onImportPlaylistM3u.invoke()
                                    }
                                )
                            }
                            ApexDropdownMenuItem(
                                text = "Quản lý thư mục",
                                icon = Icons.Rounded.MusicNote,
                                onClick = {
                                    isTopMenuOpen = false
                                    onOpenFolders()
                                }
                            )
                        }
                    }
                }
            }

                // 2. Main Filter Tabs: One UI 8.5 Borderless Pill Group (Tất cả bài hát | Album | Nghệ sĩ | Thư mục)
                item(key = "library_main_filter_tabs") {
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryTab.entries.forEach { tab ->
                            val isSelected = currentLibraryTab == tab

                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) PrimaryIvory else SurfaceControl,
                                animationSpec = spring(stiffness = 500f),
                                label = "tab_bg_anim"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) CharcoalBlack else TextSecondary,
                                animationSpec = spring(stiffness = 500f),
                                label = "tab_text_anim"
                            )

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(PillShape)
                                    .background(bgColor)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) Color.Transparent else SurfaceBorderStrong,
                                        shape = PillShape
                                    )
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                        currentLibraryTab = tab
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = textColor,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                when (currentLibraryTab) {
                    LibraryTab.SONGS -> {
                        item(key = "filter_tabs_row") {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SongSortOption.entries.forEach { option ->
                        val isSelected = currentSortOption == option

                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) PrimaryIvory else SurfaceControl,
                            animationSpec = spring(stiffness = 500f),
                            label = "filter_bg_anim"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) CharcoalBlack else TextSecondary,
                            animationSpec = spring(stiffness = 500f),
                            label = "filter_text_anim"
                        )

                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(PillShape)
                                .background(bgColor)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) Color.Transparent else SurfaceBorderStrong,
                                    shape = PillShape
                                )
                                .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                    if (currentSortOption == option) {
                                        sortAscending = !sortAscending
                                    } else {
                                        currentSortOption = option
                                        sortAscending = true
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = textColor,
                                        fontSize = 13.sp
                                    )
                                )
                                if (isSelected && option != SongSortOption.ALL) {
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                        contentDescription = "Thứ tự sắp xếp",
                                        tint = CharcoalBlack,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Action Row: Play All & Shuffle All Pills
            item(key = "quick_actions_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play All Button (Samsung Blue Pill)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(PillShape)
                            .background(PrimaryIvory)
                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                if (sortedTracks.isNotEmpty()) {
                                    onTrackSelect(sortedTracks.first(), sortedTracks)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Phát tất cả",
                                tint = CharcoalBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Phát tất cả",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                      fontWeight = FontWeight.Bold,
                                      color = CharcoalBlack,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    // Shuffle All Icon Button (Circular Glass Button)
                    ApexCircularGlassButton(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                        contentDescription = "Trộn bài",
                        onClick = onShuffleAll,
                        size = 46.dp,
                        iconSize = 22.dp,
                        backgroundColor = PrimaryIvory,
                        iconTint = CharcoalBlack
                    )
                }
            }

            // 4. Section Label & Grouped Card Container
            item(key = "songs_section_label") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMultiSelectMode) "CHỌN BÀI HÁT (${selectedTrackIds.size}/${sortedTracks.size})" else "TẤT CẢ BÀI HÁT (${sortedTracks.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isMultiSelectMode) Brand else TextSecondary,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // 5. Grouped Card Container with 120Hz LazyColumn Virtualization
            if (sortedTracks.isEmpty()) {
                item(key = "empty_library_card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceActiveIndicator),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Chưa có bài hát nào",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nhấn nút bên dưới để quét toàn bộ bài hát trên thiết bị của bạn.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(PrimaryIvory)
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) { triggerScanWithPermission() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                          imageVector = Icons.Rounded.Refresh,
                                          contentDescription = "Quét lại bài hát",
                                          tint = CharcoalBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Quét lại bài hát",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CharcoalBlack,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when (libraryViewMode) {
                    LibraryViewMode.LIST -> {
                        itemsIndexed(
                            items = sortedTracks,
                            key = { index, track -> "${track.id}_$index" },
                            contentType = { _, _ -> "track_item" }
                        ) { index, track ->

                            val isCurrent = track.id == currentTrackId
                            val isSelectedInBatch = selectedTrackIds.contains(track.id)
                            val total = sortedTracks.size

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                            ) {

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .apexBounceClick(
                                                scaleDown = 0.98f,
                                                enableHaptic = true,
                                                onLongClick = {
                                                    if (!isMultiSelectMode) {
                                                        // Nhấn giữ bài hát để mở Menu thao tác (Bước 4 OneMusic UX Plan)
                                                        trackForActions = track
                                                    }
                                                },
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        selectedTrackIds = if (isSelectedInBatch) {
                                                            selectedTrackIds - track.id
                                                        } else {
                                                            selectedTrackIds + track.id
                                                        }
                                                    } else {
                                                        onTrackSelect(track, sortedTracks)
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Multi-Select Checkbox Circle
                                        if (isMultiSelectMode) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelectedInBatch) PrimaryIvory else Color.Transparent)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelectedInBatch) PrimaryIvory else IvoryDisabled,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelectedInBatch) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Đã chọn",
                                                        tint = CharcoalBlack,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Bài không có ảnh bìa → hiện biểu tượng nốt nhạc thay vì ô trống
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(SurfaceActiveIndicator),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (track.artworkUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = track.artworkUrl,
                                                    contentDescription = track.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) Brand else PrimaryIvory,
                                                    fontSize = 15.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (appSettings.isHiResBadgeEnabled && track.isHiRes) {
                                                    ApexHiResBadge()
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }
                                                Text(
                                                    text = "${track.artist} • ${formatDuration(track.durationMs, padMinutes = true)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = TextSecondary,
                                                        fontSize = 12.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (!isMultiSelectMode) {
                                            Box(
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                                        onToggleFavorite(track.id)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                    contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                                                    tint = if (track.isFavorite) ApexRose else PrimaryIvory.copy(alpha = 0.45f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (index < total - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 102.dp, end = 16.dp),
                                            thickness = 0.6.dp,
                                            color = SurfaceDivider
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LibraryViewMode.GRID_2 -> {
                        val pairs = sortedTracks.chunked(2)
                        itemsIndexed(
                            items = pairs,
                            key = { idx, pair -> "lib_g2_${idx}_" + pair.joinToString("_") { it.id } },
                            contentType = { _, _ -> "grid2_row" }
                        ) { _, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                for (track in pair) {
                                    val isCurrent = track.id == currentTrackId
                                    val isSelectedInBatch = selectedTrackIds.contains(track.id)
                                    LibraryTrackGridCard(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isMultiSelectMode = isMultiSelectMode,
                                        isSelectedInBatch = isSelectedInBatch,
                                        isHiResBadgeEnabled = appSettings.isHiResBadgeEnabled,
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                                            } else {
                                                onTrackSelect(track, sortedTracks)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectMode) {
                                                // Nhấn giữ = bắt đầu chọn nhiều (quen thuộc trên Android), chọn sẵn bài vừa giữ
                                                isMultiSelectMode = true
                                                selectedTrackIds = setOf(track.id)
                                            }
                                        },
                                        onToggleFavorite = { onToggleFavorite(track.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    LibraryViewMode.GRID_3 -> {
                        val triplets = sortedTracks.chunked(3)
                        itemsIndexed(
                            items = triplets,
                            key = { idx, triplet -> "lib_g3_${idx}_" + triplet.joinToString("_") { it.id } },
                            contentType = { _, _ -> "grid3_row" }
                        ) { _, triplet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (track in triplet) {
                                    val isCurrent = track.id == currentTrackId
                                    val isSelectedInBatch = selectedTrackIds.contains(track.id)
                                    LibraryTrackCompactGridCard(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isMultiSelectMode = isMultiSelectMode,
                                        isSelectedInBatch = isSelectedInBatch,
                                        onClick = {
                                            if (isMultiSelectMode) {
                                                selectedTrackIds = if (isSelectedInBatch) selectedTrackIds - track.id else selectedTrackIds + track.id
                                            } else {
                                                onTrackSelect(track, sortedTracks)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isMultiSelectMode) {
                                                // Nhấn giữ = bắt đầu chọn nhiều (quen thuộc trên Android), chọn sẵn bài vừa giữ
                                                isMultiSelectMode = true
                                                selectedTrackIds = setOf(track.id)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - triplet.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

                LibraryTab.ALBUMS -> {
                    item(key = "albums_section_label") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DANH SÁCH ALBUM (${albums.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    if (albums.isEmpty()) {
                        item(key = "empty_albums") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có album nào",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        when (libraryViewMode) {
                            LibraryViewMode.GRID_2 -> {
                                val pairs = albums.chunked(2)
                                itemsIndexed(
                                    items = pairs,
                                    key = { idx, pair -> "lib_album_g2_${idx}_" + pair.joinToString("_") { it.name } }
                                ) { _, pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        for (album in pair) {
                                            val albumArtworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                        openedAlbum = album.key
                                                    }
                                            ) {
                                                // Artwork Frame
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(SurfaceActiveIndicator)
                                                ) {
                                                    if (!albumArtworkUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = albumArtworkUrl,
                                                            contentDescription = album.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(SurfaceElevated),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Album,
                                                                contentDescription = null,
                                                                tint = IvoryDisabled,
                                                                modifier = Modifier.size(52.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Album Name
                                                Text(
                                                    text = album.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        fontSize = 15.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                // Artist Name
                                                Text(
                                                    text = album.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            LibraryViewMode.GRID_3 -> {
                                val triplets = albums.chunked(3)
                                itemsIndexed(
                                    items = triplets,
                                    key = { idx, triplet -> "lib_album_g3_${idx}_" + triplet.joinToString("_") { it.name } }
                                ) { _, triplet ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        for (album in triplet) {
                                            val albumArtworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .apexGlassCard(shape = RoundedCornerShape(18.dp))
                                                    .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                                        openedAlbum = album.key
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                // Artwork Frame
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(SurfaceActiveIndicator)
                                                ) {
                                                    if (!albumArtworkUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = albumArtworkUrl,
                                                            contentDescription = album.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(SurfaceElevated),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Album,
                                                                contentDescription = null,
                                                                tint = IvoryDisabled,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Album Name
                                                Text(
                                                    text = album.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary,
                                                        fontSize = 12.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(1.dp))

                                                // Artist Name
                                                Text(
                                                    text = album.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        val remainder = 3 - triplet.size
                                        repeat(remainder) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            LibraryViewMode.LIST -> {
                                itemsIndexed(
                                    items = albums,
                                    key = { index, album -> "album_${album.key}_$index" }
                                ) { index, (albumKey, albumName, artistName, albumTracks) ->
                                    // Lấy ảnh của bài đầu tiên CÓ ảnh bìa (bài đầu album có thể không có)
                                    val albumArtworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl
                                    val total = albums.size

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .apexBounceClick(
                                                        scaleDown = 0.98f,
                                                        enableHaptic = true,
                                                        // Bấm hàng → mở trang chi tiết; nút ▶ bên phải vẫn phát nhanh
                                                        onClick = { openedAlbum = albumKey }
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Album không có ảnh bìa → hiện biểu tượng nốt nhạc thay vì ô trống
                                                Box(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(SurfaceActiveIndicator),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!albumArtworkUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = albumArtworkUrl,
                                                            contentDescription = albumName,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Rounded.MusicNote,
                                                            contentDescription = null,
                                                            tint = TextSecondary,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = albumName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            fontSize = 15.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "$artistName • ${albumTracks.size} bài hát",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = TextSecondary,
                                                            fontSize = 12.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                ApexCircularGlassButton(
                                                    icon = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Phát album",
                                                    onClick = {
                                                        if (albumTracks.isNotEmpty()) {
                                                            onPlayTracks?.invoke(albumTracks) ?: onTrackSelect(albumTracks.first(), albumTracks)
                                                        }
                                                    },
                                                    size = 38.dp,
                                                    iconSize = 20.dp,
                                                    iconTint = PrimaryIvory
                                                )
                                            }

                                            if (index < total - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                                                    thickness = 0.6.dp,
                                                    color = SurfaceDivider
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    item(key = "artists_section_label") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DANH SÁCH NGHỆ SĨ (${artists.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    if (artists.isEmpty()) {
                        item(key = "empty_artists") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có nghệ sĩ nào",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = artists,
                            key = { index, (artistName, _) -> "artist_${artistName}_$index" }
                        ) { index, (artistName, artistTracks) ->
                            val total = artists.size
                            val initials = artistName.split(" ").filter { it.isNotBlank() }.take(2)
                                .map { it.first().uppercase() }.joinToString("")
                            val avatarColor = remember(artistName) { avatarColorFor(artistName) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .apexBounceClick(
                                                scaleDown = 0.98f,
                                                enableHaptic = true,
                                                // Bấm hàng → mở trang chi tiết; nút ▶ bên phải vẫn phát nhanh
                                                onClick = { openedArtist = artistName }
                                            )
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Có ảnh nghệ sĩ (kho lưu khóa chữ thường, đã trim) → hiện ảnh; không có → giữ ô chữ viết tắt
                                        val artistImageUrl = artistImages[artistName.trim().lowercase()]
                                            ?: artistImageRepository.getCachedImageUrl(artistName)
                                        if (!artistImageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = artistImageUrl,
                                                contentDescription = artistName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .border(0.7.dp, IvoryStroke, CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(avatarColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (initials.isNotBlank()) initials else "♪",
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artistName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 15.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${artistTracks.size} bài hát",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        ApexCircularGlassButton(
                                            icon = Icons.Rounded.PlayArrow,
                                            contentDescription = "Phát tất cả của nghệ sĩ",
                                            onClick = {
                                                if (artistTracks.isNotEmpty()) {
                                                    onPlayTracks?.invoke(artistTracks) ?: onTrackSelect(artistTracks.first(), artistTracks)
                                                }
                                            },
                                            size = 38.dp,
                                            iconSize = 20.dp,
                                            iconTint = PrimaryIvory
                                        )
                                    }

                                    if (index < total - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                                            thickness = 0.6.dp,
                                            color = SurfaceDivider
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drag-to-Select Multi-items Gesture Rail (Left Checkbox Rail)
        // Chỉ ở chế độ Danh sách: findTrackIndexAtY đọc chỉ số từ khóa "${track.id}_$index" của hàng danh sách,
        // còn khóa hàng lưới ("lib_g2_…") kết thúc bằng id bài → sẽ bị hiểu nhầm thành chỉ số
        if (isMultiSelectMode && currentLibraryTab == LibraryTab.SONGS && libraryViewMode == LibraryViewMode.LIST) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(80.dp)
                    .pointerInput(isMultiSelectMode, sortedTracks) {
                        if (!isMultiSelectMode || sortedTracks.isEmpty()) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val startIdx = findTrackIndexAtY(down.position.y)
                            var dragStartIndex = startIdx
                            val initialSelection = selectedTrackIds
                            var isSelecting = if (startIdx in sortedTracks.indices) {
                                !initialSelection.contains(sortedTracks[startIdx].id)
                            } else true
                            var lastTickedIdx = startIdx
                            var isDragGesture = false

                            if (startIdx in sortedTracks.indices) {
                                val startTrackId = sortedTracks[startIdx].id
                                selectedTrackIds = if (isSelecting) initialSelection + startTrackId else initialSelection - startTrackId
                                try {
                                    hapticEngine.performGearTick(scale = 0.22f, fallbackView = currentView)
                                } catch (_: Exception) {}
                            }

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }

                                val currentY = change.position.y
                                val dy = kotlin.math.abs(currentY - down.position.y)
                                if (!isDragGesture && dy > 8f) {
                                    isDragGesture = true
                                }

                                if (isDragGesture) {
                                    change.consume()

                                    val currentIdx = findTrackIndexAtY(currentY)
                                    if (currentIdx in sortedTracks.indices) {
                                        if (dragStartIndex == -1) {
                                            dragStartIndex = currentIdx
                                            isSelecting = !initialSelection.contains(sortedTracks[currentIdx].id)
                                        }
                                        if (currentIdx != lastTickedIdx) {
                                            val minIdx = minOf(dragStartIndex, currentIdx)
                                            val maxIdx = maxOf(dragStartIndex, currentIdx)
                                            val rangeIds = (minIdx..maxIdx).map { sortedTracks[it].id }.toSet()
                                            selectedTrackIds = if (isSelecting) {
                                                initialSelection + rangeIds
                                            } else {
                                                initialSelection - rangeIds
                                            }
                                            try {
                                                hapticEngine.performGearTick(scale = 0.22f, fallbackView = currentView)
                                            } catch (_: Exception) {}
                                            lastTickedIdx = currentIdx
                                        }
                                    }

                                    // Auto-scroll when near top or bottom edges
                                    val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                                    if (viewportHeight > 0f) {
                                        if (currentY < 180f && listState.canScrollBackward) {
                                            val scrollSpeed = ((180f - currentY) / 180f * 24f).coerceIn(6f, 36f)
                                            scope.launch { listState.scrollBy(-scrollSpeed) }
                                        } else if (currentY > viewportHeight - 220f && listState.canScrollForward) {
                                            val scrollSpeed = ((currentY - (viewportHeight - 220f)) / 220f * 24f).coerceIn(6f, 36f)
                                            scope.launch { listState.scrollBy(scrollSpeed) }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Alphabet Fast Scroller (Right Edge Rail)
        // Chỉ hiện khi sắp xếp theo tên: sắp xếp "Tất cả" là thứ tự quét, không theo ABC → nhảy chữ cái vô nghĩa
        if (currentLibraryTab == LibraryTab.SONGS && !isMultiSelectMode && libraryViewMode == LibraryViewMode.LIST && sortedTracks.size >= 10 && currentSortOption == SongSortOption.TITLE_AZ) {
            ApexAlphabetScroller(
                onLetterSelected = { char: Char ->
                    val targetIndex = sortedTracks.indexOfFirst { track ->
                        val firstChar = track.title.trim().firstOrNull()?.uppercaseChar() ?: '#'
                        if (char == '#') !firstChar.isLetter() else firstChar == char
                    }

                    if (targetIndex != -1) {
                        scope.launch {
                            listState.scrollToItem(targetIndex + SONGS_HEADER_ITEM_COUNT)
                        }
                    }
                },
                availableLetters = availableLetters,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .padding(top = 246.dp, bottom = LocalBottomOverlayPadding.current, end = 2.dp)
            )
        }


        // Floating Batch Actions Pill Dock (Multi-Select Mode)
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = LocalBottomOverlayPadding.current, start = 20.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .shadow(18.dp, PillShape, ambientColor = ShadowColor)
                    .clip(PillShape)
                    .background(SurfaceElevated.copy(alpha = 0.95f))
                    .border(0.85.dp, IvoryStroke, PillShape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Counter & Close
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                .size(36.dp)
                                .clip(CircleShape)
                                .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                    isMultiSelectMode = false
                                    selectedTrackIds = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hủy", tint = PrimaryIvory, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedTrackIds.size} đã chọn",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        )
                    }

                    // Actions: Select All / Deselect, Play Selected, Add to Playlist
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Select All / Deselect Toggle
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(IvorySubtle)
                                .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                    selectedTrackIds = if (selectedTrackIds.size == sortedTracks.size) {
                                        emptySet()
                                    } else {
                                        sortedTracks.map { it.id }.toSet()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedTrackIds.size == sortedTracks.size) "Bỏ chọn" else "Tất cả",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            )
                        }

                        // Play Selected
                        if (selectedTrackIds.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(PrimaryIvory)
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                        val tracksToPlay = sortedTracks.filter { it.id in selectedTrackIds }
                                        if (tracksToPlay.isNotEmpty()) {
                                            onPlayTracks?.invoke(tracksToPlay) ?: onTrackSelect(tracksToPlay.first(), tracksToPlay)
                                            isMultiSelectMode = false
                                            selectedTrackIds = emptySet()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Phát", style = MaterialTheme.typography.labelSmall.copy(color = CharcoalBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                            }

                            // Play Next
                            if (onPlayNextTracks != null) {
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IvoryStroke)
                                        .apexBounceClick(scaleDown = 0.90f, enableHaptic = true) {
                                            val tracksToPlayNext = sortedTracks.filter { it.id in selectedTrackIds }
                                            if (tracksToPlayNext.isNotEmpty()) {
                                                onPlayNextTracks(tracksToPlayNext)
                                                isMultiSelectMode = false
                                                selectedTrackIds = emptySet()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Phát kế tiếp", tint = Brand, modifier = Modifier.size(19.dp))
                                }
                            }

                            // Add to Playlist
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IvoryStroke)
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                        showBatchAddToPlaylistDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Thêm vào playlist", tint = PrimaryIvory, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Batch Add to Playlist Dialog
        if (showBatchAddToPlaylistDialog) {
            val selectedTracks = sortedTracks.filter { it.id in selectedTrackIds }
            AddToPlaylistMultipleDialog(
                tracks = selectedTracks,
                playlists = customPlaylists,
                onCreatePlaylist = onCreatePlaylist,
                onAddToPlaylist = onAddToPlaylist,
                // Chỉ thoát chế độ chọn khi thêm thành công; bấm Hủy vẫn giữ các bài đã chọn
                onAdded = {
                    isMultiSelectMode = false
                    selectedTrackIds = emptySet()
                },
                onDismiss = { showBatchAddToPlaylistDialog = false }
            )
        }

        // Menu thao tác của bài (mở từ nút ⋮): Phát kế tiếp, Thêm vào playlist, Thông tin bài hát...
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

@Composable
private fun LibraryTrackGridCard(
    track: Track,
    isCurrent: Boolean,
    isMultiSelectMode: Boolean,
    isSelectedInBatch: Boolean,
    isHiResBadgeEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .apexBounceClick(
                scaleDown = 0.95f,
                enableHaptic = true,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Artwork Frame 1:1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceActiveIndicator)
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = IvoryDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            if (isMultiSelectMode) {
                // Multi-select Checkbox
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelectedInBatch) PrimaryIvory else ScrimColor)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelectedInBatch) PrimaryIvory else IvoryMedium,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedInBatch) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = CharcoalBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) Brand else PrimaryIvory,
                fontSize = 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Artist & Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHiResBadgeEnabled && track.isHiRes) {
                        ApexHiResBadge()
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = formatDuration(track.durationMs, padMinutes = true),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IvoryFaint,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (!isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                        .size(32.dp)
                        .clip(CircleShape)
                        .apexBounceClick(scaleDown = 0.85f, enableHaptic = true, onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                        tint = if (track.isFavorite) ApexRose else IvoryFaint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTrackCompactGridCard(
    track: Track,
    isCurrent: Boolean,
    isMultiSelectMode: Boolean,
    isSelectedInBatch: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .apexGlassCard(shape = RoundedCornerShape(18.dp))
            .apexBounceClick(
                scaleDown = 0.96f,
                enableHaptic = true,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceActiveIndicator)
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = IvoryDisabled,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelectedInBatch) PrimaryIvory else ScrimColor)
                        .border(
                            width = 1.2.dp,
                            color = if (isSelectedInBatch) PrimaryIvory else IvoryMedium,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedInBatch) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = CharcoalBlack,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) Brand else PrimaryIvory,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



