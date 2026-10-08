package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import com.example.onemusic.data.repository.ArtistImageRepository
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.avatarColorFor
import androidx.compose.foundation.lazy.LazyListScope

/** "Nghệ sĩ nổi bật": hàng avatar các nghệ sĩ nhiều bài nhất. */
internal fun LazyListScope.homeTopArtistsSection(
    topArtists: List<Pair<String, Int>>,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    onSeeAll: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    // 5. Section: "NGHỆ SĨ NGHE NHIỀU" (Top Artists)
    if (topArtists.isNotEmpty()) {
        item(key = "section_artists_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NGHỆ SĨ NỔI BẬT",
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

        // Top Artists Horizontal Avatar Row
        item(key = "artists_row") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(topArtists, key = { it.first }) { (artistName, trackCount) ->
                    // Kho ảnh lưu khóa dạng chữ thường, đã trim → tra giống các màn khác
                    val customImg = artistImages[artistName.trim().lowercase()]
                        ?: artistImageRepository.getCachedImageUrl(artistName)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(84.dp)
                            .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                                onOpenArtist(artistName)
                            }
                    ) {
                        if (!customImg.isNullOrBlank()) {
                            AsyncImage(
                                model = customImg,
                                contentDescription = artistName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, AppTheme.colors.stroke, CircleShape)
                            )
                        } else {
                            val initials = artistName.split(" ").filter { it.isNotBlank() }.take(2)
                                .map { it.first().uppercase() }.joinToString("")
                            val avatarColor = remember(artistName) { avatarColorFor(artistName) }
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (initials.isNotBlank()) initials else "♪",
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.textPrimary,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$trackCount bài",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.5.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
