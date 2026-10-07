package com.example.onemusic.ui.screens.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryBody
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Hàng tên bài + nghệ sĩ (chữ chạy khi quá dài) cùng nút ưa thích và nút ⋯ trong cụm điều khiển Now Playing.
 * [track] là bài đang hiển thị (đồng bộ với trang pager), có thể khác bài đang phát khi đang vuốt.
 */
@Composable
internal fun NowPlayingTrackHeader(
    track: Track?,
    onToggleFavorite: (trackId: String, isCurrentlyFavorite: Boolean) -> Unit,
    onOpenOptions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val legibilityTextShadow = remember {
                Shadow(color = ScrimColor, blurRadius = 8f)
            }

            Text(
                text = track?.title ?: "Không phát",
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2500,
                        initialDelayMillis = 2000,
                        spacing = androidx.compose.foundation.MarqueeSpacing(64.dp),
                        velocity = 32.dp
                    ),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = PrimaryIvory,
                    fontSize = 23.sp,
                    letterSpacing = (-0.3).sp,
                    shadow = legibilityTextShadow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track?.artist ?: "—",
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 2500,
                    initialDelayMillis = 2000,
                    spacing = androidx.compose.foundation.MarqueeSpacing(64.dp),
                    velocity = 28.dp
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = IvoryBody,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    shadow = legibilityTextShadow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action Buttons: Favorite Star Button + 3-Dot More Menu Button (1:1 Apple Music)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Inverted Contrast Favorite Button (Solid white disc with black star when active)
            val isCurrentTrackFav = track?.isFavorite == true
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentTrackFav) PrimaryIvory else IvoryStroke)
                    .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                        track?.let { trk ->
                            onToggleFavorite(trk.id, isCurrentTrackFav)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentTrackFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isCurrentTrackFav) "Bỏ yêu thích" else "Yêu thích",
                    tint = if (isCurrentTrackFav) CharcoalBlack else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Glass 3-dot More Options Button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(IvoryStroke)
                    .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                        onOpenOptions()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Tùy chọn",
                    tint = PrimaryIvory,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
