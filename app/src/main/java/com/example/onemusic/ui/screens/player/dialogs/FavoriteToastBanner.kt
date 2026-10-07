package com.example.onemusic.ui.screens.player.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.apexFrostedGlass
import dev.chrisbanes.haze.HazeState

/**
 * Thông báo nổi "Đã ưa thích" / "Đã xóa khỏi phần ưa thích" ở đáy Now Playing.
 * Hiện khi [message] khác null. Truyền `Modifier.align(Alignment.BottomCenter)` từ Box cha.
 */
@Composable
internal fun FavoriteToastBanner(
    message: String?,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier
            .padding(bottom = 36.dp)
            .navigationBarsPadding(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .border(0.7.dp, IvoryStroke, RoundedCornerShape(18.dp))
                .apexFrostedGlass(
                    backgroundColor = SurfaceElevated.copy(alpha = 0.92f),
                    blurRadius = 24.dp,
                    hazeState = hazeState
                )
                .padding(horizontal = 20.dp, vertical = 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isFavoritedMsg = message?.contains("Đã ưa thích") == true
                Icon(
                    imageVector = if (isFavoritedMsg) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = PrimaryIvory,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                )
            }
        }
    }
}
