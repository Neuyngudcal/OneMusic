package com.example.onemusic.ui.screens.dedup

import com.example.onemusic.theme.AppTheme
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.dedup.DuplicateAudioDetector
import com.example.onemusic.data.dedup.DuplicateGroup
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

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
            .background(AppTheme.colors.background)
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
                            iconTint = AppTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Lọc bài hát trùng lặp",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 30.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Phân tích thông minh 4 tầng: Tự động giữ lại bản nhạc có chất lượng cao nhất (Hi-Res/FLAC 24-bit) và bảo vệ các bản Live, Remix, Acoustic.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textSecondary,
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
                                color = AppTheme.colors.accent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang phân tích siêu dữ liệu & chất lượng âm thanh...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.textSecondary,
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
                                    .background(AppTheme.colors.surfaceActiveIndicator),
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Không có bài hát trùng lặp",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Thư viện nhạc của bạn hoàn toàn sạch sẽ, không có bài hát nào bị nhân bản hoặc trùng lặp chất lượng.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AppTheme.colors.textSecondary,
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
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Tìm thấy ${duplicateGroups.size} nhóm bài trùng lặp",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 15.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đã chọn $totalSelectedCount bài • Tiết kiệm ${Formatter.formatFileSize(context, totalReclaimableBytes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AppTheme.colors.textSecondary,
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
            DuplicateActionDock(
                totalSelectedCount = totalSelectedCount,
                // Button 1: Chỉ ẩn khỏi thư viện (An toàn)
                onHideClick = {
                    val idsToHide = selectedTrackIds.filter { it.value }.keys
                    scope.launch {
                        musicRepository.hideDuplicateTracks(context, idsToHide)
                        Toast.makeText(context, "Đã ẩn ${idsToHide.size} bài hát trùng lặp", Toast.LENGTH_SHORT).show()
                        refreshAnalysis()
                    }
                },
                // Button 2: Xóa vĩnh viễn file trên đĩa
                onDeleteClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
