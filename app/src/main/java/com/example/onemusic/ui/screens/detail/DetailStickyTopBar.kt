package com.example.onemusic.ui.screens.detail

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.components.ApexCircularGlassButton
import dev.chrisbanes.haze.HazeState

/** LAYER 3: thanh trên cùng nổi (trong suốt ở đầu trang; cuộn qua ảnh bìa thì hiện nền tối + tên). */
@Composable
internal fun DetailStickyTopBar(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    showTopTitle: Boolean,
    topBarBackgroundAlpha: () -> Float,
    hazeState: HazeState,
    customPlaylist: CustomPlaylist?,
    onBack: () -> Unit,
    onRenameClick: (CustomPlaylist) -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
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
                        onClick = { onRenameClick(customPlaylist) },
                        size = 42.dp,
                        iconSize = 19.dp,
                        hazeState = hazeState
                    )
                    ApexCircularGlassButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Xóa playlist",
                        onClick = onDeleteClick,
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
}
