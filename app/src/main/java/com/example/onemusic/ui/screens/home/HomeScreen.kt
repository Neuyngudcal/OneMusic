package com.example.onemusic.ui.screens.home

import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.utils.rememberScanWithPermission
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.components.ApexDropdownDivider
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share

import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.search.ArtistExtractor
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.screens.detail.DetailScreen
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryBody
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.theme.avatarColorFor

data class HomeCategory(
    val title: String,
    val icon: ImageVector
)

data class AlbumItemData(
    /** Khóa album (LibraryGrouping.albumKey) – dùng để mở trang chi tiết */
    val key: String,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val artworkUrl: String?,
    val tracks: List<Track>
)

data class ArtistItemData(
    val name: String,
    val trackCount: Int,
    val tracks: List<Track>
)

enum class ArtistGroupMode(val title: String, val subtitle: String) {
    MERGED("Gộp nghệ sĩ trùng tên", "Tự động gộp bài hát cùng nghệ sĩ & tách bài hợp tác (feat, &)"),
    SEPARATE("Tách riêng theo thẻ gốc", "Hiển thị nguyên bản theo từng chuỗi nghệ sĩ trong tệp")
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(java.util.Locale.US, "%d:%02d", min, sec)
}

fun extractArtistNames(rawArtist: String): List<String> = LibraryGrouping.artistNames(rawArtist)

enum class AlbumViewMode {
    LIST, GRID_2, GRID_3
}

enum class HomeSubView {
    MAIN, PLAYLISTS, ARTISTS, ALBUMS, ARTIST_DETAIL, ALBUM_DETAIL, PLAYLIST_DETAIL
}

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
            .background(ObsidianBlack)
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
                    ) {
                        // 1. Top Header: Minimalist Clean Header with Compact Status Bar Padding
                        item(key = "home_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = greetingTitle,
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 32.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = greetingSubtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }

                        // 1.2 Thư viện trống (lần mở app đầu tiên): hướng dẫn quét nhạc thay vì màn trống trơn
                        if (tracks.isEmpty()) {
                            item(key = "home_empty_library") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                        .apexGlassCard(shape = RoundedCornerShape(26.dp))
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryMusic,
                                        contentDescription = null,
                                        tint = Brand,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Thư viện đang trống",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Quét bộ nhớ máy hoặc chọn thư mục chứa nhạc để bắt đầu.",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(PrimaryIvory)
                                            .apexBounceClick { triggerScanWithPermission() }
                                            .padding(horizontal = 22.dp, vertical = 12.dp)
                                    ) {
                                        Text("Quét nhạc trên máy", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Chọn thư mục",
                                        color = PrimaryIvory,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .apexBounceClick { onOpenFolders() }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }

                        // 1.5 Hero Banner: Featured Track
                        if (featuredTrack != null) {
                            item(key = "hero_banner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                            onTrackSelect(featuredTrack, tracks)
                                        }
                                ) {
                                    // Background Artwork blurred
                                    AsyncImage(
                                        model = featuredTrack.artworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(40.dp)
                                    )
                                    
                                    // Dark Scrim Gradient
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.2f),
                                                        Color.Black.copy(alpha = 0.75f)
                                                    )
                                                )
                                            )
                                    )
                                    
                                    // Content
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(IvoryStroke)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "NỔI BẬT HÔM NAY",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                                Text(
                                                    text = featuredTrack.title,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        color = PrimaryIvory,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 22.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = featuredTrack.artist,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = IvoryBody,
                                                        fontSize = 14.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            
                                            // Play button
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryIvory),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Phát",
                                                    tint = CharcoalBlack,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Thư viện trống → ẩn "Nghe gần đây" và "Có thể bạn sẽ thích" cho màn trống gọn gàng
                        if (tracks.isNotEmpty()) {
                            // 3. Section: "NGHE GẦN ĐÂY" (Recently Played)
                            item(key = "section_recent_title") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NGHE GẦN ĐÂY",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            if (recentlyPlayedTracks.isEmpty()) {
                                item(key = "empty_recents") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .apexGlassCard(shape = RoundedCornerShape(26.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Chưa có bài hát nào được phát gần đây",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextSecondary,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }
                            } else {
                                item(key = "recent_carousel") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(recentlyPlayedTracks, key = { "recent_" + it.id }) { track ->
                                            val isCurrent = track.id == currentTrackId
                                            Column(
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .apexBounceClick(
                                                        scaleDown = 0.95f,
                                                        enableHaptic = true,
                                                        onClick = { onTrackSelect(track, recentlyPlayedTracks) },
                                                        onLongClick = { trackForActions = track }
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(SurfaceActiveIndicator)
                                                ) {
                                                    if (!track.artworkUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = track.artworkUrl,
                                                            contentDescription = track.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
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
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isCurrent) Brand else PrimaryIvory,
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(1.dp))

                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3.5 Section: CÓ THỂ BẠN SẼ THÍCH
                            if (suggestedTracks.isNotEmpty()) {
                                item(key = "section_suggested_title") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CÓ THỂ BẠN SẼ THÍCH",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary,
                                                letterSpacing = 1.sp,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                item(key = "suggested_carousel") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(suggestedTracks, key = { "suggested_" + it.id }) { track ->
                                            val isCurrent = track.id == currentTrackId
                                            Column(
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .apexBounceClick(
                                                        scaleDown = 0.95f,
                                                        enableHaptic = true,
                                                        onClick = { onTrackSelect(track, suggestedTracks) }
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(SurfaceActiveIndicator)
                                                ) {
                                                    if (!track.artworkUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = track.artworkUrl,
                                                            contentDescription = track.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
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
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isCurrent) Brand else PrimaryIvory,
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(1.dp))

                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } // end if (tracks.isNotEmpty())

                            // 4. Section: "PLAYLIST YÊU THÍCH"
                            item(key = "section_playlists_title") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PLAYLIST (${customPlaylists.size + 1})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp,
                                            fontSize = 12.sp
                                        )
                                    )
                                    Text(
                                        text = "Tất cả",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Brand,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier
                                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                currentSubView = HomeSubView.PLAYLISTS
                                            }
                                    )
                                }
                            }

                            // Horizontal Carousel of Playlists
                            item(key = "playlists_carousel") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Item 1: Favorites Playlist Card
                                    item(key = "pl_favorites") {
                                        Column(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                    isSelectedFavorites = true
                                                    selectedPlaylistId = null
                                                    selectedAlbum = null
                                                    selectedArtist = null
                                                    openDetail(HomeSubView.PLAYLIST_DETAIL)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceActiveIndicator),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Favorite,
                                                    contentDescription = null,
                                                    tint = ApexRose,
                                                    modifier = Modifier.size(56.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Yêu thích",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = "${favoriteTracks.size} bài hát",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Custom Playlists
                                    items(customPlaylists, key = { it.id }) { pl ->
                                        Column(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                    selectedPlaylistId = pl.id
                                                    isSelectedFavorites = false
                                                    selectedAlbum = null
                                                    selectedArtist = null
                                                    openDetail(HomeSubView.PLAYLIST_DETAIL)
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SurfaceActiveIndicator),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                    contentDescription = null,
                                                    tint = IvoryFaint,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = pl.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = "${pl.trackIds.size} bài hát",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Section: "NGHỆ SĨ NGHE NHIỀU" (Top Artists)
                            if (topArtists.isNotEmpty()) {
                                item(key = "section_artists_title") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "NGHỆ SĨ NỔI BẬT",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary,
                                                letterSpacing = 1.sp,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Text(
                                            text = "Tất cả",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Brand,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier
                                                .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                    currentSubView = HomeSubView.ARTISTS
                                                }
                                        )
                                    }
                                }

                                // Top Artists Horizontal Avatar Row
                                item(key = "artists_row") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(topArtists, key = { it.first }) { (artistName, trackCount) ->
                                            // Kho ảnh lưu khóa dạng chữ thường, đã trim → tra giống các màn khác
                                            val customImg = artistImages[artistName.trim().lowercase()]
                                                ?: artistImageRepository.getCachedImageUrl(artistName)

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(84.dp)
                                                    .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                                                        selectedArtist = artistName
                                                        isSelectedFavorites = false
                                                        selectedPlaylistId = null
                                                        selectedAlbum = null
                                                        openDetail(HomeSubView.ARTIST_DETAIL)
                                                    }
                                            ) {
                                                if (!customImg.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = customImg,
                                                        contentDescription = artistName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(76.dp)
                                                            .clip(CircleShape)
                                                            .border(1.dp, IvoryStroke, CircleShape)
                                                    )
                                                } else {
                                                    val initials = artistName.split(" ").filter { it.isNotBlank() }.take(2)
                                                        .map { it.first().uppercase() }.joinToString("")
                                                    val avatarColor = remember(artistName) { avatarColorFor(artistName) }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(76.dp)
                                                            .clip(CircleShape)
                                                            .background(avatarColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (initials.isNotBlank()) initials else "♪",
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 24.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = artistName,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary,
                                                        fontSize = 13.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "$trackCount bài",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = TextSecondary,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Section: "KHÁM PHÁ THÊM"
                            item(key = "section_explore_title") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "KHÁM PHÁ THƯ VIỆN",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            letterSpacing = 1.sp,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            item(key = "explore_grouped_card") {
                                val exploreCategories = listOf(
                                    HomeCategory("Album", Icons.Rounded.Album),
                                    HomeCategory("Nghệ sĩ", Icons.Rounded.Mic),
                                    HomeCategory("Playlist", Icons.AutoMirrored.Rounded.QueueMusic)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .apexGlassCard(shape = RoundedCornerShape(26.dp))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        exploreCategories.forEachIndexed { index, cat ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                                        when (cat.title) {
                                                            "Playlist" -> currentSubView = HomeSubView.PLAYLISTS
                                                            "Nghệ sĩ" -> currentSubView = HomeSubView.ARTISTS
                                                            "Album" -> currentSubView = HomeSubView.ALBUMS
                                                        }
                                                    }
                                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = cat.icon,
                                                    contentDescription = cat.title,
                                                    tint = PrimaryIvory,
                                                    modifier = Modifier.size(24.dp)
                                                )

                                                Spacer(modifier = Modifier.width(16.dp))

                                                Text(
                                                    text = cat.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        fontSize = 16.sp
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = TextSecondary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (index < exploreCategories.size - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                                                    thickness = 0.5.dp,
                                                    color = SurfaceDivider
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }




                HomeSubView.PLAYLISTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
                    ) {
                        item(key = "playlists_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ApexCircularGlassButton(
                                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                    contentDescription = "Quay lại",
                                    onClick = { currentSubView = HomeSubView.MAIN },
                                    size = 44.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Playlist",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 22.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (onImportPlaylistM3u != null) {
                                    ApexCircularGlassButton(
                                        icon = Icons.Rounded.FolderOpen,
                                        contentDescription = "Nhập playlist .m3u8",
                                        onClick = { onImportPlaylistM3u() },
                                        size = 44.dp,
                                        iconSize = 22.dp,
                                        iconTint = PrimaryIvory
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                ApexCircularGlassButton(
                                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = "Tạo playlist",
                                    onClick = { showNewPlaylistDialog = true },
                                    size = 44.dp,
                                    iconSize = 22.dp,
                                    iconTint = PrimaryIvory
                                )
                            }
                        }

                        // Favorite Playlist & Custom Playlists in One UI 8.5 Grouped Card
                        val favCount = tracks.count { it.isFavorite }
                        val totalPlaylists = 1 + customPlaylists.size

                        item(key = "favorite_playlist_item") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGroupedCardItem(index = 0, total = totalPlaylists, cornerRadius = 26.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                                isSelectedFavorites = true
                                                selectedPlaylistId = null
                                                openDetail(HomeSubView.PLAYLIST_DETAIL)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(ApexRose),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Favorite,
                                                contentDescription = null,
                                                tint = PrimaryIvory,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bài hát yêu thích",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 16.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$favCount bài hát",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                        }
                                    }
                                    if (totalPlaylists > 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                                            thickness = 0.6.dp,
                                            color = SurfaceDivider
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Playlists
                        itemsIndexed(customPlaylists, key = { index, pl -> "${pl.id}_$index" }) { idx, pl ->
                            val currentIndex = 1 + idx
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .apexGroupedCardItem(index = currentIndex, total = totalPlaylists, cornerRadius = 26.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                                selectedPlaylistId = pl.id
                                                isSelectedFavorites = false
                                                openDetail(HomeSubView.PLAYLIST_DETAIL)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(SurfaceActiveIndicator),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                contentDescription = null,
                                                tint = Brand,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pl.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 16.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${pl.trackIds.size} bài hát",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.5.sp
                                                )
                                            )
                                        }

                                        if (onExportPlaylistM3u != null) {
                                            ApexCircularGlassButton(
                                                icon = Icons.Rounded.Share,
                                                contentDescription = "Xuất playlist .m3u8",
                                                onClick = {
                                                    onExportPlaylistM3u(pl)
                                                },
                                                size = 38.dp,
                                                iconSize = 18.dp,
                                                iconTint = PrimaryIvory,
                                                backgroundColor = SurfaceActiveIndicator.copy(alpha = 0.60f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        ApexCircularGlassButton(
                                            icon = Icons.Rounded.Delete,
                                            contentDescription = "Xóa playlist",
                                            // Chỉ mở hộp xác nhận, chưa xóa ngay
                                            onClick = { playlistPendingDelete = pl },
                                            size = 38.dp,
                                            iconSize = 18.dp,
                                            iconTint = ApexRose.copy(alpha = 0.85f),
                                            backgroundColor = SurfaceActiveIndicator.copy(alpha = 0.60f)
                                        )
                                    }

                                    if (currentIndex < totalPlaylists - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 98.dp, end = 16.dp),
                                            thickness = 0.6.dp,
                                            color = SurfaceDivider
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSubView.ARTISTS -> {
                    val artistItems = remember(tracks, artistGroupMode) {
                        LibraryGrouping.groupArtists(
                            tracks,
                            mergeCollaborators = artistGroupMode == ArtistGroupMode.MERGED
                        ).map { (name, trackList) ->
                            ArtistItemData(name = name, trackCount = trackList.size, tracks = trackList)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
                        ) {
                            // 1. Header (Back, Title, Count, Layout Mode Switcher Button)
                            item(key = "artists_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Circular Back Button
                                    ApexCircularGlassButton(
                                        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                        contentDescription = "Quay lại",
                                        onClick = { currentSubView = HomeSubView.MAIN },
                                        size = 44.dp,
                                        iconSize = 20.dp
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Nghệ sĩ",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 22.sp
                                            )
                                        )
                                        Text(
                                            text = "${artistItems.size} nghệ sĩ • ${tracks.size} bài hát",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                    }

                                    // Circular Mode Switcher Button
                                    Box {
                                        ApexCircularGlassButton(
                                            icon = if (artistGroupMode == ArtistGroupMode.MERGED) Icons.Rounded.Groups else Icons.Rounded.PersonOutline,
                                            contentDescription = "Tùy chọn gộp nghệ sĩ",
                                            onClick = { isArtistGroupMenuExpanded = !isArtistGroupMenuExpanded },
                                            size = 44.dp,
                                            iconSize = 22.dp,
                                            iconTint = PrimaryIvory
                                        )

                                        ApexDropdownMenu(
                                            expanded = isArtistGroupMenuExpanded,
                                            onDismissRequest = { isArtistGroupMenuExpanded = false },
                                            hazeState = hazeState,
                                            width = 245.dp
                                        ) {
                                            ApexDropdownMenuItem(
                                                text = "Gộp nghệ sĩ trùng tên",
                                                icon = Icons.Rounded.Groups,
                                                trailingText = if (artistGroupMode == ArtistGroupMode.MERGED) "✓" else null,
                                                trailingColor = Brand,
                                                textColor = if (artistGroupMode == ArtistGroupMode.MERGED) Brand else TextPrimary,
                                                onClick = {
                                                    artistGroupMode = ArtistGroupMode.MERGED
                                                    isArtistGroupMenuExpanded = false
                                                }
                                            )
                                            ApexDropdownDivider()
                                            ApexDropdownMenuItem(
                                                text = "Tách riêng theo thẻ gốc",
                                                icon = Icons.Rounded.PersonOutline,
                                                trailingText = if (artistGroupMode == ArtistGroupMode.SEPARATE) "✓" else null,
                                                trailingColor = Brand,
                                                textColor = if (artistGroupMode == ArtistGroupMode.SEPARATE) Brand else TextPrimary,
                                                onClick = {
                                                    artistGroupMode = ArtistGroupMode.SEPARATE
                                                    isArtistGroupMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }


                            // 2. Artists List
                            if (artistItems.isEmpty()) {
                                item(key = "artists_empty") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = IvoryMuted,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Không có nghệ sĩ nào trong thư viện",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 14.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                val total = artistItems.size
                                itemsIndexed(artistItems, key = { index, it -> "${it.name}_$index" }) { index, artistItem ->
                                    val artistImageUrl = artistImages[artistItem.name.trim().lowercase()]
                                        ?: artistImageRepository.getCachedImageUrl(artistItem.name)

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
                                                    .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                                        selectedArtist = artistItem.name
                                                        openDetail(HomeSubView.ARTIST_DETAIL)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!artistImageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = artistImageUrl,
                                                        contentDescription = artistItem.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(CircleShape)
                                                            .border(0.7.dp, IvoryStroke, CircleShape)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(CircleShape)
                                                            .background(SurfaceActiveIndicator),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Mic,
                                                            contentDescription = null,
                                                            tint = PrimaryIvory,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = artistItem.name,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            fontSize = 16.sp
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${artistItem.trackCount} bài hát",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = TextSecondary,
                                                            fontSize = 13.5.sp
                                                        )
                                                    )
                                                }
                                            }
                                            if (index < total - 1) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 94.dp, end = 16.dp),
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


                HomeSubView.ALBUMS -> {
                    // Gộp album bằng hàm dùng chung: theo (tên album, nghệ sĩ chính), sắp xếp A→Z
                    val rawAlbums = remember(tracks) {
                        LibraryGrouping.groupAlbums(tracks).map { album ->
                            AlbumItemData(
                                key = album.key,
                                name = album.name,
                                artist = album.artist,
                                trackCount = album.tracks.size,
                                artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
                                tracks = album.tracks
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
                        ) {
                            // 1. HEADER (Back, Title, Count, Layout Mode Switcher Button)
                            item(key = "albums_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Circular Back Button
                                    ApexCircularGlassButton(
                                        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                        contentDescription = "Quay lại",
                                        onClick = { currentSubView = HomeSubView.MAIN },
                                        size = 44.dp,
                                        iconSize = 20.dp
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Album",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 22.sp
                                            )
                                        )
                                        Text(
                                            text = "${rawAlbums.size} album • ${tracks.size} bài hát",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                    }

                                    // Circular Layout Switcher Button
                                    val currentModeIcon = when (albumViewMode) {
                                        AlbumViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
                                        AlbumViewMode.GRID_2 -> Icons.Rounded.GridView
                                        AlbumViewMode.GRID_3 -> Icons.Rounded.ViewModule
                                    }

                                    Box {
                                        ApexCircularGlassButton(
                                            icon = currentModeIcon,
                                            contentDescription = "Tùy chọn hiển thị",
                                            onClick = { isAlbumViewMenuExpanded = !isAlbumViewMenuExpanded },
                                            size = 44.dp,
                                            iconSize = 22.dp
                                        )

                                        ApexDropdownMenu(
                                            expanded = isAlbumViewMenuExpanded,
                                            onDismissRequest = { isAlbumViewMenuExpanded = false },
                                            hazeState = hazeState,
                                            width = 205.dp
                                        ) {
                                            ApexDropdownMenuItem(
                                                text = "Danh sách",
                                                icon = Icons.AutoMirrored.Rounded.ViewList,
                                                trailingText = if (albumViewMode == AlbumViewMode.LIST) "✓" else null,
                                                trailingColor = Brand,
                                                textColor = if (albumViewMode == AlbumViewMode.LIST) Brand else TextPrimary,
                                                onClick = {
                                                    albumViewMode = AlbumViewMode.LIST
                                                    isAlbumViewMenuExpanded = false
                                                }
                                            )
                                            ApexDropdownDivider()
                                            ApexDropdownMenuItem(
                                                text = "Lưới 2 cột",
                                                icon = Icons.Rounded.GridView,
                                                trailingText = if (albumViewMode == AlbumViewMode.GRID_2) "✓" else null,
                                                trailingColor = Brand,
                                                textColor = if (albumViewMode == AlbumViewMode.GRID_2) Brand else TextPrimary,
                                                onClick = {
                                                    albumViewMode = AlbumViewMode.GRID_2
                                                    isAlbumViewMenuExpanded = false
                                                }
                                            )
                                            ApexDropdownDivider()
                                            ApexDropdownMenuItem(
                                                text = "Lưới 3 cột",
                                                icon = Icons.Rounded.ViewModule,
                                                trailingText = if (albumViewMode == AlbumViewMode.GRID_3) "✓" else null,
                                                trailingColor = Brand,
                                                textColor = if (albumViewMode == AlbumViewMode.GRID_3) Brand else TextPrimary,
                                                onClick = {
                                                    albumViewMode = AlbumViewMode.GRID_3
                                                    isAlbumViewMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }


                            // 2. ALBUMS CONTENT BY VIEW MODE
                            if (rawAlbums.isEmpty()) {
                                item(key = "albums_empty") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Rounded.Album,
                                                contentDescription = null,
                                                tint = IvoryMuted,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Không có album nào trong thư viện",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 14.5.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                when (albumViewMode) {
                                    AlbumViewMode.LIST -> {
                                        val total = rawAlbums.size
                                        itemsIndexed(rawAlbums, key = { index, it -> "list_${it.name}_$index" }) { index, album ->
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
                                                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                                                selectedAlbum = album.key
                                                                openDetail(HomeSubView.ALBUM_DETAIL)
                                                            }
                                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Artwork
                                                        if (!album.artworkUrl.isNullOrBlank()) {
                                                            AsyncImage(
                                                                model = album.artworkUrl,
                                                                contentDescription = album.name,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .size(64.dp)
                                                                    .clip(RoundedCornerShape(16.dp))
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(64.dp)
                                                                    .clip(RoundedCornerShape(16.dp))
                                                                    .background(SurfaceElevated),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Album,
                                                                    contentDescription = null,
                                                                    tint = IvoryFaint,
                                                                    modifier = Modifier.size(34.dp)
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(14.dp))

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = album.name,
                                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextPrimary,
                                                                    fontSize = 16.sp
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "${album.artist} • ${album.trackCount} bài hát",
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    color = TextSecondary,
                                                                    fontSize = 13.5.sp
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        // Quick play button
                                                        Box(
                                                            modifier = Modifier
                                                                .size(38.dp)
                                                                .clip(CircleShape)
                                                                .background(PrimaryIvory)
                                                                .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                                                    onPlayTracks(album.tracks)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.PlayArrow,
                                                                contentDescription = "Phát nhanh",
                                                                tint = CharcoalBlack,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    if (index < total - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(start = 94.dp, end = 16.dp),
                                                            thickness = 0.6.dp,
                                                            color = SurfaceDivider
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    AlbumViewMode.GRID_2 -> {
                                        val pairs = rawAlbums.chunked(2)
                                        itemsIndexed(pairs, key = { idx, pair -> "g2_${idx}_" + pair.joinToString("_") { it.name } }) { _, pair ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                for (album in pair) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                                selectedAlbum = album.key
                                                                openDetail(HomeSubView.ALBUM_DETAIL)
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
                                                            if (!album.artworkUrl.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = album.artworkUrl,
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

                                    AlbumViewMode.GRID_3 -> {
                                        val triplets = rawAlbums.chunked(3)
                                        itemsIndexed(triplets, key = { idx, triplet -> "g3_${idx}_" + triplet.joinToString("_") { it.name } }) { _, triplet ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                for (album in triplet) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                                selectedAlbum = album.key
                                                                openDetail(HomeSubView.ALBUM_DETAIL)
                                                            }
                                                    ) {
                                                        // Artwork Frame
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(SurfaceActiveIndicator)
                                                        ) {
                                                            if (!album.artworkUrl.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = album.artworkUrl,
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
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary,
                                                                fontSize = 13.sp
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
                                                                fontSize = 11.5.sp
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                val emptySlots = 3 - triplet.size
                                                for (i in 0 until emptySlots) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                HomeSubView.ARTIST_DETAIL -> {
                    val artistTracks = remember(selectedArtist, tracks, artistGroupMode) {
                        val target = selectedArtist.orEmpty()
                        if (artistGroupMode == ArtistGroupMode.MERGED) {
                            tracks.filter { track ->
                                extractArtistNames(track.artist).any {
                                    it.equals(target, ignoreCase = true) ||
                                    target.startsWith("$it ", ignoreCase = true) ||
                                    target.startsWith("${it}x", ignoreCase = true)
                                }
                            }
                        } else {
                            tracks.filter { it.artist.equals(target, ignoreCase = true) }
                        }
                    }
                    val artistImageUrl = selectedArtist?.let {
                        artistImages[it.trim().lowercase()] ?: artistImageRepository.getCachedImageUrl(it)
                    }
                    DetailScreen(
                        title = selectedArtist.orEmpty(),
                        subtitle = "Nghệ sĩ",
                        artworkUrl = artistImageUrl,
                        tracks = artistTracks,
                        currentTrackId = currentTrackId,
                        isArtist = true,
                        onTrackSelect = onTrackSelect,
                        onPlayAll = onPlayTracks,
                        onShuffleAll = { onPlayTracks(it.shuffled()) },
                        onToggleFavorite = onToggleFavorite,
                        onOpenAddToPlaylist = onOpenAddToPlaylist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onBack = { currentSubView = detailBackTarget }
                    )
                }

                HomeSubView.ALBUM_DETAIL -> {
                    val albumTracks = remember(selectedAlbum, tracks) {
                        LibraryGrouping.tracksOfAlbum(tracks, selectedAlbum.orEmpty())
                    }
                    DetailScreen(
                        title = albumTracks.firstOrNull()?.let { LibraryGrouping.albumName(it) } ?: "Album",
                        subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
                        artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
                        tracks = albumTracks,
                        currentTrackId = currentTrackId,
                        onTrackSelect = onTrackSelect,
                        onPlayAll = onPlayTracks,
                        onShuffleAll = { onPlayTracks(it.shuffled()) },
                        onToggleFavorite = onToggleFavorite,
                        onOpenAddToPlaylist = onOpenAddToPlaylist,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onBack = { currentSubView = detailBackTarget }
                    )
                }

                HomeSubView.PLAYLIST_DETAIL -> {
                    if (isSelectedFavorites) {
                        val favTracks = remember(tracks) { tracks.filter { it.isFavorite } }
                        DetailScreen(
                            title = "Bài hát yêu thích",
                            subtitle = "Yêu thích",
                            isFavorites = true,
                            tracks = favTracks,
                            currentTrackId = currentTrackId,
                            onTrackSelect = onTrackSelect,
                            onPlayAll = onPlayTracks,
                            onShuffleAll = { onPlayTracks(it.shuffled()) },
                            onToggleFavorite = onToggleFavorite,
                            onOpenAddToPlaylist = onOpenAddToPlaylist,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onBack = { currentSubView = detailBackTarget }
                        )
                    } else {
                        val activePl = customPlaylists.find { it.id == selectedPlaylistId }
                        val plTracks = remember(activePl, tracks) {
                            val map = tracks.associateBy { it.id }
                            activePl?.trackIds?.mapNotNull { map[it] } ?: emptyList()
                        }
                        DetailScreen(
                            title = activePl?.name ?: "Danh sách phát",
                            subtitle = "Danh sách phát",
                            customPlaylist = activePl,
                            tracks = plTracks,
                            currentTrackId = currentTrackId,
                            onTrackSelect = onTrackSelect,
                            onPlayAll = onPlayTracks,
                            onShuffleAll = { onPlayTracks(it.shuffled()) },
                            onToggleFavorite = onToggleFavorite,
                            onOpenAddToPlaylist = onOpenAddToPlaylist,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                            onAddTrackToPlaylist = onAddTrackToPlaylist,
                            onRenamePlaylist = onRenamePlaylist,
                            onDeletePlaylist = { id ->
                                onDeletePlaylist(id)
                                currentSubView = detailBackTarget
                            },
                            onBack = { currentSubView = detailBackTarget }
                        )
                    }
                }
            }
        }

        // New Playlist Dialog
        if (showNewPlaylistDialog) {
            ApexDialogContainer(
                onDismissRequest = { showNewPlaylistDialog = false },
                hazeState = hazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Tạo Danh Sách Phát Mới",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Tên danh sách phát...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Brand,
                            unfocusedBorderColor = SurfaceDivider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(SurfaceControl)
                                .border(1.5.dp, SurfaceBorderStrong, PillShape)
                                .apexBounceClick(scaleDown = 0.92f) { showNewPlaylistDialog = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Hủy", color = PrimaryIvory, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(PrimaryIvory)
                                .apexBounceClick(scaleDown = 0.92f) {
                                    if (newPlaylistName.isNotBlank()) {
                                        onCreatePlaylist(newPlaylistName.trim())
                                        newPlaylistName = ""
                                    }
                                    showNewPlaylistDialog = false
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("Tạo", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

