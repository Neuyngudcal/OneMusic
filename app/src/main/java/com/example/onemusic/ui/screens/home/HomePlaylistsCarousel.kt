package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.IvoryFaint
import androidx.compose.foundation.lazy.LazyListScope

/** Hàng ngang "Playlist": thẻ Yêu thích + các playlist tự tạo. */
internal fun LazyListScope.homePlaylistsCarouselSection(
    customPlaylists: List<CustomPlaylist>,
    favoriteTrackCount: Int,
    onSeeAll: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPlaylist: (CustomPlaylist) -> Unit
) {
    // 4. Section: "PLAYLIST YÊU THÍCH"
    item(key = "section_playlists_title") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PLAYLIST (${customPlaylists.size + 1})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            )
            Text(
                text = "Tất cả",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.accent,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                        onSeeAll()
                    }
            )
        }
    }

    // Horizontal Carousel of Playlists
    item(key = "playlists_carousel") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Item 1: Favorites Playlist Card
            item(key = "pl_favorites") {
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                            onOpenFavorites()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = ApexRose,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Yêu thích",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "$favoriteTrackCount bài hát",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Custom Playlists
            items(customPlaylists, key = { it.id }) { pl ->
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                            onOpenPlaylist(pl)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = IvoryFaint,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pl.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${pl.trackIds.size} bài hát",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
