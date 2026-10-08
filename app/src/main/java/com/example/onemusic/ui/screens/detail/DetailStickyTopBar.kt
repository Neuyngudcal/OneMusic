package com.example.onemusic.ui.screens.detail

import com.example.onemusic.theme.AppTheme
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.rememberArtworkTopIsLight

/** LAYER 3: thanh trên cùng nổi (trong suốt ở đầu trang; cuộn qua ảnh bìa thì hiện nền tối + tên). */
@Composable
internal fun DetailStickyTopBar(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    showTopTitle: Boolean,
    topBarBackgroundAlpha: () -> Float,
    artworkUrl: String?,
    isOverArtwork: Boolean,
    customPlaylist: CustomPlaylist?,
    onBack: () -> Unit,
    onRenameClick: (CustomPlaylist) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    // Icon đen khi nằm trên phần ảnh bìa sáng; còn lại (ảnh tối, chưa có ảnh, đã cuộn qua ảnh) là trắng
    val artworkTopIsLight = rememberArtworkTopIsLight(artworkUrl)
    val iconTint by animateColorAsState(
        targetValue = if (artworkTopIsLight == true && isOverArtwork) AppTheme.colors.onInverse else AppTheme.colors.textPrimary,
        label = "detail_top_bar_icon_tint"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Nền đặt TRƯỚC statusBarsPadding để phủ cả vùng thanh trạng thái
            .background(ObsidianBlack.copy(alpha = topBarBackgroundAlpha()))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = "Quay lại",
                onClick = onBack,
                tint = iconTint
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
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Nút Chia sẻ / Tùy chọn góc phải
            if (customPlaylist != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopBarIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Đổi tên",
                        onClick = { onRenameClick(customPlaylist) },
                        tint = iconTint
                    )
                    TopBarIconButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Xóa playlist",
                        onClick = onDeleteClick,
                        tint = ApexRose
                    )
                }
            } else {
                TopBarIconButton(
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
                    tint = iconTint
                )
            }
        }
    }
}

/** Nút chỉ có icon (không nền tròn), vùng chạm 48dp. */
@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .apexBounceClick(scaleDown = 0.85f, enableHaptic = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
