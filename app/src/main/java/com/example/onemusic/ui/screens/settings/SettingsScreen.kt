package com.example.onemusic.ui.screens.settings

import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.SearchPreferences
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.ArtistImageRepository

import com.example.onemusic.theme.LocalApexHazeState
import dev.chrisbanes.haze.HazeState
import com.example.onemusic.ui.components.ApexConfirmDialog

import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.ui.components.ApexEqualizerDialog
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard

/**
 * OneMusic Apex Prism Settings Screen
 * - Expansive collapsible large viewing area with large title "Cài đặt".
 * - Grouped Card Containers with SurfaceCard, SurfaceDivider, apexGlassCard.
 * - Full toggles for all audio, playback, UI, haptics, and library features.
 */
@Composable
fun SettingsScreen(
    settingsPreferences: SettingsPreferences,
    onBack: () -> Unit,
    audioEffectManager: AudioEffectManager? = null,
    onOpenFolders: () -> Unit,
    onOpenDuplicateCleaner: () -> Unit = {},
    onRescan: () -> Unit = {},
    onClearMotionCache: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showSnackbar = com.example.onemusic.ui.utils.LocalAppSnackbar.current
    val listState = rememberLazyListState()
    val settings by settingsPreferences.settingsFlow.collectAsState()
    val searchPrefs = remember { SearchPreferences(context) }
    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }
    // Xin quyền đọc nhạc (nếu chưa có) rồi mới quét
    val triggerScanWithPermission = com.example.onemusic.ui.utils.rememberScanWithPermission(onRescan)
    val hapticEngine = com.example.onemusic.haptics.rememberApexHaptics()
    val effectManager = audioEffectManager ?: remember { AudioEffectManager(settingsPreferences) }

    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }
    var showClearArtistCacheDialog by remember { mutableStateOf(false) }
    var showClearMotionCacheDialog by remember { mutableStateOf(false) }
    // Giá trị Crossfade tạm trong lúc kéo thanh trượt (đồng bộ lại khi giá trị đã lưu thay đổi)
    var crossfadeDraft by remember(settings.crossfadeDurationSeconds) {
        mutableFloatStateOf(settings.crossfadeDurationSeconds.toFloat())
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
            // 1. Clean Samsung One UI Top Header
            item(key = "settings_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Cài đặt",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 32.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tùy chỉnh & âm thanh",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            settingsAudioSection(
                settings = settings,
                settingsPreferences = settingsPreferences,
                crossfadeDraft = { crossfadeDraft },
                onCrossfadeDraftChange = { crossfadeDraft = it },
                onOpenEqualizer = { showEqualizerDialog = true }
            )

            settingsDisplaySection(settings, settingsPreferences)

            settingsHapticsSection(settings, settingsPreferences, hapticEngine)

            settingsLibrarySection(
                settings = settings,
                settingsPreferences = settingsPreferences,
                onOpenFolders = onOpenFolders,
                onOpenDuplicateCleaner = onOpenDuplicateCleaner,
                // Trước đây gọi onRescan() không kiểm tra quyền → không có quyền thì im lặng không làm gì
                onRescan = { triggerScanWithPermission() },
                onClearArtistCache = { showClearArtistCacheDialog = true },
                onClearMotionCache = { showClearMotionCacheDialog = true },
                onClearSearchHistory = { showClearSearchDialog = true }
            )

            settingsAboutSection()
        }

        // Dialog Confirm Clear Artist Images Cache
        if (showClearArtistCacheDialog) {
            ApexConfirmDialog(
                title = "Xóa bộ nhớ đệm ảnh nghệ sĩ?",
                message = "Toàn bộ ảnh đại diện nghệ sĩ đã tải về từ Internet sẽ được xóa. Ứng dụng sẽ tự động tải lại ảnh mới khi bạn mở thư viện.",
                confirmButtonText = "Xóa bộ nhớ đệm",
                dismissButtonText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    ArtistImageRepository.getInstance(context).clearCache()
                    showClearArtistCacheDialog = false
                    showSnackbar("Đã xóa bộ nhớ đệm ảnh nghệ sĩ", null, null)
                },
                onDismiss = { showClearArtistCacheDialog = false }
            )
        }

        // Dialog Confirm Clear Motion Artwork Cache
        if (showClearMotionCacheDialog) {
            ApexConfirmDialog(
                title = "Xóa bộ nhớ đệm bìa động?",
                message = "Toàn bộ video bìa chuyển động (Motion Art) đã tải về sẽ bị xóa khỏi máy để làm mới và giải phóng bộ nhớ.",
                confirmButtonText = "Xóa bộ nhớ đệm",
                dismissButtonText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    onClearMotionCache?.invoke()
                    showClearMotionCacheDialog = false
                    showSnackbar("Đã xóa bộ nhớ đệm bìa động", null, null)
                },
                onDismiss = { showClearMotionCacheDialog = false }
            )
        }

        // Dialog Confirm Clear Search History
        if (showClearSearchDialog) {
            ApexConfirmDialog(
                title = "Xóa lịch sử tìm kiếm?",
                message = "Toàn bộ danh sách từ khóa tìm kiếm gần đây sẽ bị xóa hoàn toàn khỏi thiết bị.",
                confirmButtonText = "Xóa lịch sử",
                dismissButtonText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    searchPrefs.clearAllRecentSearches()
                    showClearSearchDialog = false
                    showSnackbar("Đã xóa lịch sử tìm kiếm", null, null)
                },
                onDismiss = { showClearSearchDialog = false }
            )
        }

        // Dialog Equalizer & SoundAlive DSP
        if (showEqualizerDialog) {
            ApexEqualizerDialog(
                audioEffectManager = effectManager,
                onDismiss = { showEqualizerDialog = false }
            )
        }
    }
}