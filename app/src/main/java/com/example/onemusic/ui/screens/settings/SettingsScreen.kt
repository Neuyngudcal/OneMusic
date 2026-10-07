package com.example.onemusic.ui.screens.settings

import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.local.HapticIntensity
import com.example.onemusic.data.local.SearchPreferences
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.ArtistImageRepository

import com.example.onemusic.theme.LocalApexHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.components.ApexSlider
import com.example.onemusic.ui.components.ApexToggle
import com.example.onemusic.ui.utils.apexBounceClick
import kotlin.math.roundToInt

import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.ui.components.ApexEqualizerDialog
import androidx.compose.material.icons.rounded.Tune
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceControl
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

            // ================================================================
            // SECTION 1: ÂM THANH & PHÁT NHẠC (AUDIO & PLAYBACK)
            // ================================================================
            item(key = "header_audio") { SettingsSectionHeader("Âm thanh & phát nhạc") }

            item(key = "section_card_audio") {
                SettingsGroupCard {
                    // Equalizer & SoundAlive DSP
                    SettingsActionRow(
                        icon = Icons.Rounded.Tune,
                        title = "Bộ chỉnh âm",
                        subtitle = "Chỉnh 9 dải tần, tăng bass và chọn cấu hình SoundAlive",
                        onClick = { showEqualizerDialog = true }
                    )

                    SettingsDivider()

                    // ReplayGain (EBU R128)
                    SettingsToggleRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Cân bằng âm lượng",
                        subtitle = "Giữ các bài to nhỏ đều nhau (ReplayGain / EBU R128)",
                        checked = settings.isReplayGainEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isReplayGainEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Headroom Limiter
                    SettingsToggleRow(
                        icon = Icons.Rounded.Security,
                        title = "Chống vỡ tiếng",
                        subtitle = "Tránh rè tiếng khi tăng âm lượng bài hát (Headroom Limiter)",
                        checked = settings.isHeadroomLimiterEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isHeadroomLimiterEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Gapless Playback
                    SettingsToggleRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "Phát liền mạch",
                        subtitle = "Chuyển bài không có khoảng lặng (Gapless)",
                        checked = settings.isGaplessPlaybackEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isGaplessPlaybackEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Crossfade
                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        title = "Chuyển bài mượt",
                        subtitle = "Hòa dần cuối bài cũ vào đầu bài mới (Crossfade)",
                        checked = settings.isCrossfadeEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isCrossfadeEnabled = isChecked) }
                        }
                    )

                    // Crossfade Duration Slider (Visible when enabled)
                    AnimatedVisibility(
                        visible = settings.isCrossfadeEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Thời gian làm mờ",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "${crossfadeDraft.roundToInt()} giây",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Brand,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ApexSlider(
                                value = crossfadeDraft,
                                // Trong lúc kéo chỉ đổi giá trị tạm; thả tay mới ghi vào bộ nhớ
                                onValueChange = { crossfadeDraft = it },
                                onValueChangeFinished = {
                                    settingsPreferences.updateSettings {
                                        it.copy(crossfadeDurationSeconds = crossfadeDraft.roundToInt())
                                    }
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    SettingsDivider()

                    // Pause on Headset Disconnect
                    SettingsToggleRow(
                        icon = Icons.Rounded.Headphones,
                        title = "Dừng khi rút tai nghe",
                        subtitle = "Tạm dừng khi ngắt tai nghe có dây hoặc Bluetooth",
                        checked = settings.pauseOnHeadsetDisconnect,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(pauseOnHeadsetDisconnect = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Resume on Headset Connect
                    SettingsToggleRow(
                        icon = Icons.Rounded.Headphones,
                        title = "Phát khi cắm tai nghe",
                        subtitle = "Phát tiếp khi kết nối lại tai nghe hoặc Bluetooth",
                        checked = settings.resumeOnHeadsetConnect,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(resumeOnHeadsetConnect = isChecked) }
                        }
                    )
                }
            }

            // ================================================================
            // SECTION 2: GIAO DIỆN & HIỂN THỊ (DISPLAY & UI)
            // ================================================================
            item(key = "header_ui") { SettingsSectionHeader("Giao diện") }

            item(key = "section_card_ui") {
                SettingsGroupCard {
                    // Dynamic Mesh Background
                    SettingsToggleRow(
                        icon = Icons.Rounded.Palette,
                        title = "Nền màu động",
                        subtitle = "Nền chuyển màu theo ảnh bìa; tắt để dùng nền đen",
                        checked = settings.isDynamicMeshBackgroundEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isDynamicMeshBackgroundEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Motion Artwork (Apple Music)
                    SettingsToggleRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Bìa động",
                        subtitle = "Phát video bìa cho bài hát có hỗ trợ",
                        checked = settings.isMotionArtworkEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isMotionArtworkEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Auto Motion Artwork Background Scan
                    SettingsToggleRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Tải trước bìa động",
                        subtitle = "Tải bìa động cho cả thư viện; tắt để tiết kiệm dữ liệu, pin",
                        checked = settings.isAutoMotionScanEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isAutoMotionScanEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Online Lyrics (LRCLIB)
                    SettingsToggleRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "Tải lời bài hát",
                        subtitle = "Tự tải lời từ Internet khi tệp nhạc không có lời",
                        checked = settings.isOnlineLyricsEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isOnlineLyricsEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Auto Download Artist Images
                    SettingsToggleRow(
                        icon = Icons.Rounded.Person,
                        title = "Tải ảnh nghệ sĩ",
                        subtitle = "Tự tải ảnh ca sĩ từ Internet",
                        checked = settings.isAutoDownloadArtistImagesEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isAutoDownloadArtistImagesEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Keep Screen On
                    SettingsToggleRow(
                        icon = Icons.Rounded.Fullscreen,
                        title = "Giữ màn hình sáng",
                        subtitle = "Không tắt màn hình khi đang mở màn Đang phát",
                        checked = settings.isKeepScreenOnEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isKeepScreenOnEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Hi-Res Badge
                    SettingsToggleRow(
                        icon = Icons.Rounded.Check,
                        title = "Huy hiệu Hi-Res",
                        subtitle = "Đánh dấu bài chất lượng cao (FLAC, 24-bit…)",
                        checked = settings.isHiResBadgeEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isHiResBadgeEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Hide Status Bar
                    SettingsToggleRow(
                        icon = Icons.Rounded.Fullscreen,
                        title = "Ẩn thanh trạng thái",
                        subtitle = "Ẩn giờ, pin và thông báo ở đỉnh màn hình",
                        checked = settings.isHideStatusBarEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isHideStatusBarEnabled = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Hide Navigation Bar
                    SettingsToggleRow(
                        icon = Icons.Rounded.Navigation,
                        title = "Ẩn thanh điều hướng",
                        subtitle = "Ẩn thanh ở đáy màn hình; vuốt lên từ cạnh dưới để hiện lại",
                        checked = settings.isHideNavigationBarEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isHideNavigationBarEnabled = isChecked) }
                        }
                    )
                }
            }

            // ================================================================
            // SECTION 3: PHẢN HỒI XÚC GIÁC (HAPTICS)
            // ================================================================
            item(key = "header_haptics") { SettingsSectionHeader("Rung phản hồi") }

            item(key = "section_card_haptics") {
                SettingsGroupCard {
                    // Haptic Feedback Toggle
                    SettingsToggleRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Rung khi chạm",
                        subtitle = "Rung nhẹ khi bấm nút và vuốt",
                        checked = settings.isHapticFeedbackEnabled,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(isHapticFeedbackEnabled = isChecked) }
                        }
                    )

                    // Haptic Intensity Selector (Visible when enabled)
                    AnimatedVisibility(
                        visible = settings.isHapticFeedbackEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Mức độ rung",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticIntensity.entries.forEach { intensity ->
                                    val isSelected = settings.hapticIntensity == intensity
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(PillShape)
                                            .background(if (isSelected) PrimaryIvory else SurfaceControl)
                                            .border(1.5.dp, if (isSelected) Color.Transparent else SurfaceBorderStrong, PillShape)
                                            .apexBounceClick(scaleDown = 0.94f, enableHaptic = false) {
                                                settingsPreferences.updateSettings {
                                                    it.copy(hapticIntensity = intensity)
                                                }
                                                hapticEngine.performCrispTap()
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = intensity.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) CharcoalBlack else PrimaryIvory,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================================================================
            // SECTION 4: THƯ VIỆN & BỘ NHỚ (LIBRARY & STORAGE)
            // ================================================================
            item(key = "header_library") { SettingsSectionHeader("Thư viện & bộ nhớ") }

            item(key = "section_card_library") {
                SettingsGroupCard {
                    // Filter Short Audio Files
                    SettingsToggleRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "Bỏ qua tệp ngắn",
                        subtitle = "Không quét tệp dưới 30 giây (nhạc chuông, ghi âm…)",
                        checked = settings.filterShortAudio,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(filterShortAudio = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Auto-scan on launch
                    SettingsToggleRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Quét khi mở ứng dụng",
                        subtitle = "Tìm bài hát mới mỗi lần mở OneMusic",
                        checked = settings.autoScanOnLaunch,
                        onCheckedChange = { isChecked ->
                            settingsPreferences.updateSettings { it.copy(autoScanOnLaunch = isChecked) }
                        }
                    )

                    SettingsDivider()

                    // Folder Manager Navigation
                    SettingsActionRow(
                        icon = Icons.Rounded.Folder,
                        title = "Thư mục nhạc",
                        subtitle = "Chọn thư mục để quét nhạc",
                        onClick = onOpenFolders
                    )

                    SettingsDivider()

                    // Duplicate Cleaner
                    SettingsActionRow(
                        icon = Icons.Rounded.CopyAll,
                        title = "Dọn bài trùng lặp",
                        subtitle = "Tìm và dọn các bản nhạc bị trùng",
                        onClick = onOpenDuplicateCleaner
                    )

                    SettingsDivider()

                    // Rescan Library Now
                    SettingsActionRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Quét lại thư viện",
                        subtitle = "Cập nhật danh sách bài hát và thông tin bài",
                        // Trước đây gọi onRescan() không kiểm tra quyền → không có quyền thì im lặng không làm gì
                        onClick = { triggerScanWithPermission() }
                    )

                    SettingsDivider()

                    // Clear Artist Images Cache
                    SettingsActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        title = "Xóa ảnh nghệ sĩ đã tải",
                        subtitle = "Ảnh sẽ được tải lại khi cần",
                        titleColor = ApexRose,
                        onClick = { showClearArtistCacheDialog = true }
                    )

                    SettingsDivider()

                    // Clear Motion Artwork Cache
                    SettingsActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        title = "Xóa bìa động đã tải",
                        subtitle = "Giải phóng dung lượng; bìa sẽ được tải lại khi cần",
                        titleColor = ApexRose,
                        onClick = { showClearMotionCacheDialog = true }
                    )

                    SettingsDivider()

                    // Clear Search History
                    SettingsActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        title = "Xóa lịch sử tìm kiếm",
                        subtitle = "Xóa các từ khóa tìm kiếm gần đây",
                        titleColor = ApexRose,
                        onClick = { showClearSearchDialog = true }
                    )
                }
            }

            // ================================================================
            // SECTION 5: THÔNG TIN ỨNG DỤNG (ABOUT & SYSTEM)
            // ================================================================
            item(key = "header_about") { SettingsSectionHeader("Thông tin") }

            item(key = "section_card_about") {
                SettingsGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceActiveIndicator),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Brand,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OneMusic",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 17.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Phiên bản 1.0.0 (OneMusic Apex Prism Edition)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ExoPlayer Engine • EBU R128 ReplayGain DSP • Tactile Haptics",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Brand,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                        }
                    }
                }
            }
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

/** Tiêu đề nhóm cài đặt (chữ in hoa nhỏ phía trên mỗi thẻ). */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        ),
        modifier = Modifier
            .padding(start = 36.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.8.dp,
        color = SurfaceDivider
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val hapticEngine = com.example.onemusic.haptics.rememberApexHaptics()
    val currentView = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "settings_toggle_row_scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            // Cả hàng là một công tắc: TalkBack đọc "…, Công tắc, Đang bật/tắt"; nảy nhẹ như các nút khác (không gợn sóng)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = { newValue ->
                    try {
                        hapticEngine.performConfirmation(scale = 0.45f, fallbackView = currentView)
                    } catch (_: Exception) {}
                    onCheckedChange(newValue)
                }
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (checked) Brand.copy(alpha = 0.20f) else SurfaceActiveIndicator),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) Brand else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Hàng đã khai báo trạng thái công tắc → ẩn công tắc con khỏi TalkBack để không đọc hai lần
        ApexToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceActiveIndicator),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != TextPrimary) titleColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}

