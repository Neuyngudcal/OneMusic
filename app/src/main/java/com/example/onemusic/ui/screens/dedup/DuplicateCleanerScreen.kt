package com.example.onemusic.ui.screens.dedup

import androidx.compose.material3.minimumInteractiveComponentSize

import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.dedup.DuplicateAudioDetector
import com.example.onemusic.data.dedup.DuplicateGroup
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.utils.apexBounceClick
import kotlinx.coroutines.launch
import com.example.onemusic.theme.ApexPillBorderBrush
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.AvatarGreen
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.IvoryHairline
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ShadowColor
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard

@Composable
fun DuplicateCleanerScreen(
    musicRepository: MusicRepository,
    playerController: MusicPlayerController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackState by playerController.playbackState.collectAsState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var isAnalyzing by remember { mutableStateOf(true) }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    val selectedTrackIds = remember { mutableStateMapOf<String, Boolean>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun refreshAnalysis() {
        scope.launch {
            isAnalyzing = true
            selectedTrackIds.clear()
            val groups = musicRepository.getDuplicateGroups(context)
            duplicateGroups = groups
            // Mặc định chọn tất cả các bài trùng lặp để xử lý
            for (g in groups) {
                for (d in g.duplicateTracks) {
                    selectedTrackIds[d.id] = true
                }
            }
            isAnalyzing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAnalysis()
    }

    BackHandler {
        onBack()
    }

    val totalSelectedCount = selectedTrackIds.values.count { it }
    val totalReclaimableBytes = duplicateGroups.sumOf { group ->
        group.duplicateTracks
            .filter { selectedTrackIds[it.id] == true }
            .sumOf { DuplicateAudioDetector.getTrackFileSize(context, it) }
    }

    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(ObsidianBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp + navBottom)
        ) {
            // 1. Top Bar with Back Button & Title
            item(key = "dedup_top_bar") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ApexCircularGlassButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = "Quay lại",
                            onClick = onBack,
                            size = 44.dp,
                            iconSize = 20.dp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        ApexCircularGlassButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "Quét lại",
                            onClick = { refreshAnalysis() },
                            size = 44.dp,
                            iconSize = 22.dp,
                            iconTint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Lọc bài hát trùng lặp",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 30.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Phân tích thông minh 4 tầng: Tự động giữ lại bản nhạc có chất lượng cao nhất (Hi-Res/FLAC 24-bit) và bảo vệ các bản Live, Remix, Acoustic.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    )
                }
            }

            // 2. Analyzing Banner
            if (isAnalyzing) {
                item(key = "analyzing_indicator") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Brand,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang phân tích siêu dữ liệu & chất lượng âm thanh...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            } else if (duplicateGroups.isEmpty()) {
                item(key = "empty_duplicates") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp)
                            .apexGlassCard(shape = RoundedCornerShape(26.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceActiveIndicator),
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Brand,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Không có bài hát trùng lặp",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Thư viện nhạc của bạn hoàn toàn sạch sẽ, không có bài hát nào bị nhân bản hoặc trùng lặp chất lượng.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 3. Statistics Summary Card
                item(key = "summary_card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .apexGlassCard(shape = RoundedCornerShape(22.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = Brand,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Tìm thấy ${duplicateGroups.size} nhóm bài trùng lặp",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 15.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đã chọn $totalSelectedCount bài • Tiết kiệm ${Formatter.formatFileSize(context, totalReclaimableBytes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Duplicate Groups List
                itemsIndexed(
                    items = duplicateGroups,
                    key = { index, group -> "${group.groupKey}_$index" }
                ) { _, group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedTrackIds = selectedTrackIds,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onToggleTrack = { trackId ->
                            val current = selectedTrackIds[trackId] ?: false
                            selectedTrackIds[trackId] = !current
                        },
                        onPlayPreview = { track ->
                            val isCurrent = playbackState.currentTrack?.id == track.id
                            if (isCurrent && playbackState.isPlaying) {
                                playerController.togglePlayPause()
                            } else {
                                playerController.setQueue(listOf(track), startIndex = 0, autoPlay = true)
                            }
                        }
                    )
                }
            }
        }

        // 5. Floating Bottom Action Dock
        if (!isAnalyzing && duplicateGroups.isNotEmpty() && totalSelectedCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
                    .shadow(16.dp, PillShape, ambientColor = ShadowColor)
                    .clip(PillShape)
                    .border(0.85.dp, ApexPillBorderBrush, PillShape)
                    .background(SurfaceElevated.copy(alpha = 0.94f))
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button 1: Chỉ ẩn khỏi thư viện (An toàn)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(SurfaceControl)
                            .border(1.5.dp, SurfaceBorderStrong, PillShape)
                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                val idsToHide = selectedTrackIds.filter { it.value }.keys
                                scope.launch {
                                    musicRepository.hideDuplicateTracks(context, idsToHide)
                                    Toast.makeText(context, "Đã ẩn ${idsToHide.size} bài hát trùng lặp", Toast.LENGTH_SHORT).show()
                                    refreshAnalysis()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = PrimaryIvory,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ẩn ($totalSelectedCount)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIvory,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }

                    // Button 2: Xóa vĩnh viễn file trên đĩa
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(ApexRose.copy(alpha = 0.18f))
                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                showDeleteConfirmDialog = true
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = ApexRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xóa File ($totalSelectedCount)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ApexRose,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            ApexConfirmDialog(
                title = "Xóa $totalSelectedCount file trùng lặp?",
                message = "Các tệp âm thanh này sẽ bị xóa vĩnh viễn khỏi thiết bị để giải phóng dung lượng bộ nhớ. Thao tác này không thể hoàn tác.",
                confirmButtonText = "Xóa vĩnh viễn",
                dismissButtonText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    val idsToDelete = selectedTrackIds.filter { it.value }.keys
                    scope.launch {
                        val deleted = musicRepository.deleteDuplicateFiles(context, idsToDelete)
                        Toast.makeText(context, "Đã xóa thành công $deleted file nhạc trùng lặp", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = false
                        refreshAnalysis()
                    }
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedTrackIds: Map<String, Boolean>,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onToggleTrack: (String) -> Unit,
    onPlayPreview: (Track) -> Unit
) {
    val context = LocalContext.current
    val primary = group.primaryTrack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .apexGlassCard(shape = RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PRIMARY TRACK (Khuyên giữ lại)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AvatarGreen.copy(alpha = 0.14f))
                    .border(0.6.dp, AvatarGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = primary.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AvatarGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GIỮ LẠI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    fontSize = 9.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = primary.bitRate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AvatarGreen,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val primarySize = DuplicateAudioDetector.getTrackFileSize(context, primary)
                    Text(
                        text = "${formatDuration(primary.durationMs)} • ${Formatter.formatFileSize(context, primarySize)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }

                // Play preview button
                val isCurrent = currentPlayingTrackId == primary.id
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) Brand else IvorySubtle)
                        .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                            onPlayPreview(primary)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isCurrent && isPlaying) "Tạm dừng nghe thử" else "Nghe thử",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DUPLICATE TRACKS (Đề xuất lọc)
            group.duplicateTracks.forEachIndexed { index, dupTrack ->
                val isSelected = selectedTrackIds[dupTrack.id] ?: false
                val isCurrent = currentPlayingTrackId == dupTrack.id
                val dupSize = DuplicateAudioDetector.getTrackFileSize(context, dupTrack)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) ApexRose.copy(alpha = 0.15f) else SurfaceCard)
                        .border(
                            0.5.dp,
                            if (isSelected) ApexRose.copy(alpha = 0.4f) else IvoryHairline,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleTrack(dupTrack.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ApexRose else IvoryStroke),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceActiveIndicator)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "TRÙNG LẶP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        fontSize = 8.5.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dupTrack.bitRate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = IvoryHigh,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${formatDuration(dupTrack.durationMs)} • ${Formatter.formatFileSize(context, dupSize)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Play preview
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) Brand else IvorySubtle)
                            .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                onPlayPreview(dupTrack)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isCurrent && isPlaying) "Tạm dừng nghe thử" else "Nghe thử",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

