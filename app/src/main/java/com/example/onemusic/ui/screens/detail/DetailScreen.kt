package com.example.onemusic.ui.screens.detail

import androidx.compose.material3.minimumInteractiveComponentSize

import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.onemusic.R
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.theme.apexGroupedCardItem

private fun formatTotalDuration(tracks: List<Track>): String {
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

    // Dải Gradient AMOLED kéo dài mượt mà từ startY = 240f đến endY = 1550f
    val bottomScrim = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                ObsidianBlack.copy(alpha = 0.40f),
                ObsidianBlack.copy(alpha = 0.85f),
                ObsidianBlack
            ),
            startY = 240f,
            endY = 1550f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // LAYER 1: HERO BACKGROUND LAYER (520dp - Góc nhìn điện ảnh tràn viền)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            if (isFavorites) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ApexRose.copy(alpha = 0.35f), ObsidianBlack)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = ApexRose.copy(alpha = 0.45f),
                        modifier = Modifier
                            .padding(bottom = 120.dp)
                            .size(130.dp)
                            .graphicsLayer {
                                val offset = listState.firstVisibleItemScrollOffset
                                if (listState.firstVisibleItemIndex == 0) {
                                    translationY = -offset * 0.45f
                                    alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                                }
                            }
                    )
                }
            } else if (!artworkUrl.isNullOrBlank()) {
                // Ảnh bìa tràn viền 100% toàn chiều ngang màn hình (Full-Bleed FillWidth), hiển thị trọn vẹn không bị cắt 2 bên
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = title,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val offset = listState.firstVisibleItemScrollOffset
                            if (listState.firstVisibleItemIndex == 0) {
                                translationY = -offset * 0.45f
                                alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                            }
                        }
                )
            } else if (isArtist) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = IvoryMuted,
                        modifier = Modifier
                            .padding(bottom = 120.dp)
                            .size(130.dp)
                            .graphicsLayer {
                                val offset = listState.firstVisibleItemScrollOffset
                                if (listState.firstVisibleItemIndex == 0) {
                                    translationY = -offset * 0.45f
                                    alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                                }
                            }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = IvoryMuted,
                        modifier = Modifier
                            .padding(bottom = 120.dp)
                            .size(130.dp)
                            .graphicsLayer {
                                val offset = listState.firstVisibleItemScrollOffset
                                if (listState.firstVisibleItemIndex == 0) {
                                    translationY = -offset * 0.45f
                                    alpha = (1f - (offset / 900f)).coerceIn(0f, 1f)
                                }
                            }
                    )
                }
            }

            // Lớp làm mềm chân ảnh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomScrim)
            )
        }

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 300.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ObsidianBlack.copy(alpha = 0.85f),
                                    ObsidianBlack,
                                    ObsidianBlack
                                )
                            )
                        )
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tên Album
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 26.sp,
                            lineHeight = 32.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Tên Ca sĩ & Huy hiệu Hi-Res
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = IvoryHigh,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tracks.any { it.isHiRes }) {
                            Spacer(modifier = Modifier.width(8.dp))
                            ApexHiResBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Metadata số bài & thời lượng
                    Text(
                        text = if (tracks.isNotEmpty()) "${tracks.size} bài hát • ${formatTotalDuration(tracks)}" else subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // NÚT "PHÁT TẤT CẢ" + NÚT TRỘN BÀI
                    Row(
                        modifier = Modifier.fillMaxWidth(0.82f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(PillShape)
                                .background(PrimaryIvory)
                                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                                    if (tracks.isNotEmpty()) {
                                        onPlayAll(tracks)
                                    } else {
                                        showSnackbar("Danh sách này chưa có bài hát nào", null, null)
                                    }
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = CharcoalBlack,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Phát tất cả",
                                color = CharcoalBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                        }

                        ApexCircularGlassButton(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                            contentDescription = "Trộn bài",
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    onShuffleAll(tracks)
                                } else {
                                    showSnackbar("Danh sách này chưa có bài hát nào", null, null)
                                }
                            },
                            size = 48.dp,
                            iconSize = 22.dp,
                            backgroundColor = PrimaryIvory,
                            iconTint = CharcoalBlack
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // 2. Danh Sách Bài Hát Grouped Cards (Nền đen ObsidianBlack bao phủ toàn diện)
            val total = tracks.size
            itemsIndexed(
                items = tracks,
                key = { index, track -> "${track.id}_$index" },
                contentType = { _, _ -> "track_item" }
            ) { index, track ->
                val isCurrent = track.id == currentTrackId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianBlack)
                        .padding(horizontal = 20.dp)
                        .apexGroupedCardItem(index = index, total = total)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .apexBounceClick(
                                    scaleDown = 0.98f,
                                    enableHaptic = true,
                                    onLongClick = { trackForActions = track }
                                ) {
                                    onTrackSelect(track, tracks)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isCurrent) Brand else TextSecondary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.width(28.dp)
                            )

                            if (track.artworkUrl.isNotBlank()) {
                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceActiveIndicator),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Brand,
                                        modifier = Modifier.size(26.dp)
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
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .apexFrostedGlass(
                                        backgroundColor = SurfaceElevated.copy(alpha = 0.60f),
                                        blurRadius = 12.dp,
                                        hazeState = hazeState
                                    )
                                    .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                        onToggleFavorite(track.id)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                                    tint = if (track.isFavorite) ApexRose else TextSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Xóa khỏi playlist (nếu custom playlist)
                            if (customPlaylist != null && onRemoveTrackFromPlaylist != null) {
                                // Khoảng cách rộng hơn để giảm bấm nhầm giữa nút tim và nút xóa
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .apexFrostedGlass(
                                            backgroundColor = SurfaceElevated.copy(alpha = 0.60f),
                                            blurRadius = 12.dp,
                                            hazeState = hazeState
                                        )
                                        .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                            onRemoveTrackFromPlaylist(customPlaylist.id, track.id)
                                            // Thao tác nhẹ → làm ngay + cho "Hoàn tác" (bài sẽ quay về CUỐI playlist)
                                            showSnackbar(
                                                "Đã xóa \"${track.title}\" khỏi \"${customPlaylist.name}\"",
                                                if (onAddTrackToPlaylist != null) "Hoàn tác" else null
                                            ) {
                                                onAddTrackToPlaylist?.invoke(customPlaylist.id, track.id)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Xóa khỏi playlist",
                                        tint = ApexRose.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (index < tracks.size - 1) {
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

        // LAYER 3: FLOATING STICKY TOP BAR (Trong suốt khi ở đầu trang; cuộn qua ảnh bìa thì hiện nền tối + tên)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Nền đặt TRƯỚC statusBarsPadding để phủ cả vùng thanh trạng thái
                .background(ObsidianBlack.copy(alpha = topBarBackgroundAlpha))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApexCircularGlassButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Quay lại",
                    onClick = onBack,
                    size = 42.dp,
                    iconSize = 19.dp,
                    hazeState = hazeState
                )

                // Tên album/nghệ sĩ/playlist – chỉ hiện khi đã cuộn qua phần ảnh bìa
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showTopTitle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Nút Chia sẻ / Tùy chọn kính mờ góc phải
                if (customPlaylist != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = "Đổi tên",
                            onClick = {
                                renameInput = customPlaylist.name
                                showRenameDialog = true
                            },
                            size = 42.dp,
                            iconSize = 19.dp,
                            hazeState = hazeState
                        )
                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = "Xóa playlist",
                            onClick = { showDeleteConfirmDialog = true },
                            size = 42.dp,
                            iconSize = 19.dp,
                            iconTint = ApexRose,
                            hazeState = hazeState
                        )
                    }
                } else {
                    ApexCircularGlassButton(
                        icon = Icons.Rounded.Share,
                        contentDescription = "Chia sẻ",
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "$title - $subtitle (${tracks.size} bài hát)")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ"))
                            } catch (_: Exception) {}
                        },
                        size = 42.dp,
                        iconSize = 19.dp,
                        hazeState = hazeState
                    )
                }
            }
        }

        // LAYER 4: DIALOGS
        if (showRenameDialog && customPlaylist != null && onRenamePlaylist != null) {
            ApexDialogContainer(onDismissRequest = { showRenameDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Đổi tên danh sách phát",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
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
                                .apexBounceClick(scaleDown = 0.92f) { showRenameDialog = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Hủy", color = PrimaryIvory, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(PrimaryIvory)
                                .apexBounceClick(scaleDown = 0.92f) {
                                    if (renameInput.isNotBlank()) {
                                        // Tên mới hiện ngay trên màn → không cần thông báo
                                        onRenamePlaylist(customPlaylist.id, renameInput.trim())
                                    }
                                    showRenameDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Lưu", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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


