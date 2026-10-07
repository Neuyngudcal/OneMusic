package com.example.onemusic.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGlassCard
import androidx.compose.foundation.lazy.LazyListScope

/** "Khám phá thư viện": lối tắt sang màn Album / Nghệ sĩ / Playlist. */
internal fun LazyListScope.homeExploreSection(
    onNavigate: (HomeSubView) -> Unit
) {
    // 6. Section: "KHÁM PHÁ THÊM"
    item(key = "section_explore_title") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KHÁM PHÁ THƯ VIỆN",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                )
            )
        }
    }

    item(key = "explore_grouped_card") {
        val exploreCategories = listOf(
            HomeCategory("Album", Icons.Rounded.Album),
            HomeCategory("Nghệ sĩ", Icons.Rounded.Mic),
            HomeCategory("Playlist", Icons.AutoMirrored.Rounded.QueueMusic)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .apexGlassCard(shape = RoundedCornerShape(26.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                exploreCategories.forEachIndexed { index, cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                when (cat.title) {
                                    "Playlist" -> onNavigate(HomeSubView.PLAYLISTS)
                                    "Nghệ sĩ" -> onNavigate(HomeSubView.ARTISTS)
                                    "Album" -> onNavigate(HomeSubView.ALBUMS)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.title,
                            tint = PrimaryIvory,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (index < exploreCategories.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = SurfaceDivider
                        )
                    }
                }
            }
        }
    }
}
