package com.example.onemusic.ui.screens.folder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.documentfile.provider.DocumentFile
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.components.ApexConfirmDialog
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.onemusic.theme.ApexAmber
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ShadowColor
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard

@Composable
fun FolderManagerScreen(
    musicRepository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folders by musicRepository.folders.collectAsState()
    val isScanning by musicRepository.isScanning.collectAsState()
    val allTracks by musicRepository.tracks.collectAsState(initial = emptyList())
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }
    // (uriString, displayName) của thư mục đang chờ xác nhận xóa
    var folderPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }

    BackHandler {
        onBack()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val docFile = DocumentFile.fromTreeUri(context, uri)
            val displayName = docFile?.name ?: uri.lastPathSegment ?: "Thư mục nhạc"
            // Chỉ báo quét (isScanning) của màn này đã báo tiến trình, không cần Toast
            musicRepository.addAndScanFolder(context, uri, displayName)
        }
    }

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
            item(key = "folder_top_bar") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Thư mục nhạc",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 32.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Chọn các thư mục chứa nhạc trên thiết bị để ứng dụng quét và tự động phát nhạc Hi-Res chất lượng cao.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // 2. Scanning Progress Banner
            item(key = "scanning_banner") {
                AnimatedVisibility(
                    visible = isScanning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = SurfaceCard
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Brand,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Đang quét tệp âm thanh...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Trích xuất thông số Hi-Res & Album artwork",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Brand
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Grouped Card: Danh sách thư mục đã chọn
            item(key = "folders_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "THƯ MỤC ĐANG QUẢN LÝ (${folders.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    ) {
                        if (folders.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
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
                                        imageVector = Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = ApexAmber,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Chưa có thư mục nào",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 16.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Nhấn nút 'Thêm thư mục mới' bên dưới để quét các bài hát yêu thích của bạn.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                folders.forEachIndexed { index, folder ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(SurfaceActiveIndicator),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Folder,
                                                contentDescription = null, // biểu tượng trang trí, tên thư mục đã có ở cạnh
                                                tint = ApexAmber,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = folder.displayName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary,
                                                    fontSize = 15.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${folder.trackCount} bài hát tìm thấy",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        ApexCircularGlassButton(
                                            icon = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Xóa thư mục",
                                            // Chỉ mở hộp xác nhận, chưa xóa ngay
                                            onClick = { folderPendingRemove = folder.uriString to folder.displayName },
                                            size = 38.dp,
                                            iconSize = 18.dp,
                                            iconTint = ApexRose.copy(alpha = 0.85f),
                                            backgroundColor = SurfaceActiveIndicator.copy(alpha = 0.60f)
                                        )
                                    }

                                    if (index < folders.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 74.dp, end = 18.dp),
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

            // 4. Grouped Card: Thông tin thư viện & Định dạng
            item(key = "memory_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "THÔNG TIN BỘ NHỚ & ĐỊNH DẠNG",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexGlassCard(shape = RoundedCornerShape(26.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Row 1: Tổng số bài hát
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AudioFile,
                                    contentDescription = null,
                                    tint = Brand,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Tổng số bài hát khả dụng",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${allTracks.size} bài",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Brand,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp, end = 18.dp),
                                thickness = 0.5.dp,
                                color = IvorySubtle
                            )

                            // Row 2: Định dạng hỗ trợ
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SdCard,
                                    contentDescription = null,
                                    tint = ApexAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Định dạng giải mã",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "FLAC 24-bit, WAV 192kHz, DSD, ALAC, MP3, AAC, M4A",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Action Buttons (Pills)
            item(key = "action_pills") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 1: Thêm thư mục mới (Primary Ivory Pill with Spring Bounce)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, PillShape, ambientColor = ShadowColor)
                            .clip(PillShape)
                            .background(PrimaryIvory)
                            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                folderPickerLauncher.launch(null)
                            }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = CharcoalBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thêm thư mục mới",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = CharcoalBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    // Button 2: Quét lại toàn bộ (Secondary Ivory Bordered Pill with Spring Bounce)
                    if (folders.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, PillShape, ambientColor = ShadowColor)
                                .clip(PillShape)
                                .background(SurfaceControl)
                                .border(1.5.dp, SurfaceBorderStrong, PillShape)
                                .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                    if (!isScanning) {
                                        musicRepository.rescanAllMusic(context)
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = PrimaryIvory,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Quét lại toàn bộ thư mục",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PrimaryIvory,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Xác nhận trước khi bỏ thư mục quét (bài hát sẽ biến mất khỏi thư viện)
        folderPendingRemove?.let { (uriString, displayName) ->
            ApexConfirmDialog(
                title = "Xóa thư mục \"$displayName\"?",
                message = "Các bài hát trong thư mục này sẽ biến mất khỏi thư viện OneMusic (file gốc không bị xóa).",
                confirmButtonText = "Xóa",
                isDestructive = true,
                onConfirm = {
                    musicRepository.removeFolder(context, uriString)
                    folderPendingRemove = null
                },
                onDismiss = { folderPendingRemove = null }
            )
        }
    }
}

