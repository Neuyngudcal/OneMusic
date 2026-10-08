package com.example.onemusic.ui.components

import androidx.compose.ui.graphics.takeOrElse
import com.example.onemusic.theme.AppTheme
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexReflectiveBorderBrush
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.TextPrimary

/**
 * OneMusic Apex Prism Track Action Sheet (1:1 Apple Music Context Menu)
 *
 * Mandatory Specs (GEMINI.md 3.7 & User Rules):
 * - Header: Squircle album art (58dp, corner 14dp), Track title (White), Artist (Brand #228B22), Album (TextSecondary).
 * - 100% Icons: Pure White (#FFFFFF) for standard features.
 * - "Xóa khỏi hàng đợi" đặt cuối, chữ trắng (không phải thao tác phá hủy).
 * - Real-time GPU Haze frosted glass surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApexTrackActionSheet(
    track: Track,
    onDismissRequest: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onDeleteTrack: ((Track) -> Unit)? = null,
    onOpenCredits: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    onPinTrack: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    hasMotionArtwork: Boolean = false,
    onRemoveMotionArtwork: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effectiveHazeState = hazeState ?: LocalApexHazeState.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = ScrimColor,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .border(
                    width = 0.85.dp,
                    brush = ApexReflectiveBorderBrush,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .apexFrostedGlass(
                    backgroundColor = AppTheme.colors.surface1.copy(alpha = 0.94f),
                    blurRadius = 28.dp,
                    hazeState = effectiveHazeState
                )
                .navigationBarsPadding()
        ) {
            // Subtle Drag Handle [ — ]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.5.dp)
                        .clip(CircleShape)
                        .background(IvoryMuted)
                )
            }

            // 1. Header Component (Artwork + Title + Artist [Brand] + Album)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Squircle Album Thumbnail (58dp x 58dp, bo góc 14dp)
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.surface1),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(track.artworkUrl)
                                .crossfade(true)
                                .size(128, 128)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = IvoryFaint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Tên ca sĩ mang màu nhấn thương hiệu Brand (#228B22) theo chuẩn quy định
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (track.album.isNotBlank()) track.album else "Đơn khúc",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.textTertiary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Thanh phân cách rõ ràng, to bản và nổi bật hơn cho dễ nhìn
            HorizontalDivider(
                color = IvoryStroke,
                thickness = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Action Items List – chỉ hiện các mục CÓ chức năng thật (nút không làm gì còn tệ hơn không có nút)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                // Phát kế tiếp / Thêm vào cuối hàng đợi – để ĐẦU danh sách (thao tác hay dùng nhất)
                if (onPlayNext != null) {
                    ActionSheetRow(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Phát kế tiếp",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onPlayNext(track)
                            }
                        }
                    )
                }
                if (onAddToQueue != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.AddToQueue,
                        label = "Thêm vào cuối hàng đợi",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onAddToQueue(track)
                            }
                        }
                    )
                }

                // Ghim bài hát – chỉ khi có chức năng ghim thật
                if (onPinTrack != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.PushPin,
                        label = "Ghim bài hát",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onPinTrack(track)
                            }
                        }
                    )
                }

                // Thêm vào playlist...
                if (onAddToPlaylist != null) {
                    ActionSheetRow(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        label = "Thêm vào playlist...",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onAddToPlaylist(track)
                            }
                        }
                    )
                }

                // Chia sẻ bài hát
                ActionSheetRow(
                    icon = Icons.Rounded.Share,
                    label = "Chia sẻ bài hát",
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, track.title)
                                putExtra(Intent.EXTRA_TEXT, "Đang nghe \"${track.title}\" của ${track.artist} trên OneMusic")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ bài hát"))
                        }
                    }
                )

                // Chia sẻ lời bài hát – chỉ khi bài có lời, và gửi kèm lời thật
                if (track.lyrics.isNotEmpty()) {
                    ActionSheetRow(
                        icon = Icons.Rounded.FormatQuote,
                        label = "Chia sẻ lời bài hát",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                val lyricsText = track.lyrics.joinToString("\n") { it.text }.take(1500)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Lời bài hát: ${track.title}")
                                    putExtra(Intent.EXTRA_TEXT, "${track.title} – ${track.artist}\n\n$lyricsText")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ lời bài hát"))
                            }
                        }
                    )
                }

                // Thông tin bài hát (dialog thông số kỹ thuật: bitrate, tần số...)
                if (onOpenCredits != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.Info,
                        label = "Thông tin bài hát",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onOpenCredits()
                            }
                        }
                    )
                }

                // Hẹn giờ tắt
                if (onOpenSleepTimer != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.Bedtime,
                        label = "Hẹn giờ tắt",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onOpenSleepTimer()
                            }
                        }
                    )
                }

                // Hoàn tác ưa thích / Ưa thích
                val isFav = track.isFavorite
                ActionSheetRow(
                    icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isFav) "Hoàn tác ưa thích" else "Ưa thích",
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                            onToggleFavorite(track.id)
                        }
                    }
                )

                // Gỡ bìa động khỏi bài hát (nếu có motion artwork)
                if (hasMotionArtwork && onRemoveMotionArtwork != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.VisibilityOff,
                        label = "Gỡ bìa động khỏi bài hát",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onRemoveMotionArtwork.invoke()
                                Toast.makeText(context, "Đã gỡ bìa động khỏi bài hát", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Xóa khỏi hàng đợi – để CUỐI (người dùng hay bấm các mục đầu). Ở Now Playing onDeleteTrack
                // chỉ bỏ bài khỏi hàng đợi, không phải thao tác phá hủy → chữ trắng, không đỏ.
                if (onDeleteTrack != null) {
                    ActionSheetRow(
                        icon = Icons.Rounded.RemoveCircleOutline,
                        label = "Xóa khỏi hàng đợi",
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onDeleteTrack(track)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified
) {
    val iconTint = iconTint.takeOrElse { AppTheme.colors.textPrimary }
    val textColor = textColor.takeOrElse { AppTheme.colors.textPrimary }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
