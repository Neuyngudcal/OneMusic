package com.example.onemusic.ui.screens.home

import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.data.repository.ArtistImageRepository
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.components.ApexDropdownDivider
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.components.ApexCircularGlassButton
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGroupedCardItem

/** HomeSubView.ARTISTS: danh sách nghệ sĩ, menu chọn gộp/tách nghệ sĩ. Chế độ gộp và trạng thái menu do màn cha giữ. */
@Composable
internal fun HomeArtistsContent(
    tracks: List<Track>,
    artistGroupMode: ArtistGroupMode,
    onArtistGroupModeChange: (ArtistGroupMode) -> Unit,
    isGroupMenuExpanded: Boolean,
    onGroupMenuExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    artistImages: Map<String, String>,
    artistImageRepository: ArtistImageRepository,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val artistItems = remember(tracks, artistGroupMode) {
        LibraryGrouping.groupArtists(
            tracks,
            mergeCollaborators = artistGroupMode == ArtistGroupMode.MERGED
        ).map { (name, trackList) ->
            ArtistItemData(name = name, trackCount = trackList.size, tracks = trackList)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. Header (Back, Title, Count, Layout Mode Switcher Button)
            item(key = "artists_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Back Button
                    ApexCircularGlassButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = "Quay lại",
                        onClick = onBack,
                        size = 44.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nghệ sĩ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "${artistItems.size} nghệ sĩ • ${tracks.size} bài hát",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.5.sp
                            )
                        )
                    }

                    // Circular Mode Switcher Button
                    Box {
                        ApexCircularGlassButton(
                            icon = if (artistGroupMode == ArtistGroupMode.MERGED) Icons.Rounded.Groups else Icons.Rounded.PersonOutline,
                            contentDescription = "Tùy chọn gộp nghệ sĩ",
                            onClick = { onGroupMenuExpandedChange(!isGroupMenuExpanded) },
                            size = 44.dp,
                            iconSize = 22.dp,
                            iconTint = PrimaryIvory
                        )

                        ApexDropdownMenu(
                            expanded = isGroupMenuExpanded,
                            onDismissRequest = { onGroupMenuExpandedChange(false) },
                            hazeState = hazeState,
                            width = 245.dp
                        ) {
                            ApexDropdownMenuItem(
                                text = "Gộp nghệ sĩ trùng tên",
                                icon = Icons.Rounded.Groups,
                                trailingText = if (artistGroupMode == ArtistGroupMode.MERGED) "✓" else null,
                                trailingColor = Brand,
                                textColor = if (artistGroupMode == ArtistGroupMode.MERGED) Brand else TextPrimary,
                                onClick = {
                                    onArtistGroupModeChange(ArtistGroupMode.MERGED)
                                    onGroupMenuExpandedChange(false)
                                }
                            )
                            ApexDropdownDivider()
                            ApexDropdownMenuItem(
                                text = "Tách riêng theo thẻ gốc",
                                icon = Icons.Rounded.PersonOutline,
                                trailingText = if (artistGroupMode == ArtistGroupMode.SEPARATE) "✓" else null,
                                trailingColor = Brand,
                                textColor = if (artistGroupMode == ArtistGroupMode.SEPARATE) Brand else TextPrimary,
                                onClick = {
                                    onArtistGroupModeChange(ArtistGroupMode.SEPARATE)
                                    onGroupMenuExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }


            // 2. Artists List
            if (artistItems.isEmpty()) {
                item(key = "artists_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = IvoryMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Không có nghệ sĩ nào trong thư viện",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 14.5.sp
                                )
                            )
                        }
                    }
                }
            } else {
                val total = artistItems.size
                itemsIndexed(artistItems, key = { index, it -> "${it.name}_$index" }) { index, artistItem ->
                    val artistImageUrl = artistImages[artistItem.name.trim().lowercase()]
                        ?: artistImageRepository.getCachedImageUrl(artistItem.name)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                                        onOpenArtist(artistItem.name)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!artistImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = artistImageUrl,
                                        contentDescription = artistItem.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .border(0.7.dp, IvoryStroke, CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceActiveIndicator),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Mic,
                                            contentDescription = null,
                                            tint = PrimaryIvory,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artistItem.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${artistItem.trackCount} bài hát",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextSecondary,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                            }
                            if (index < total - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 94.dp, end = 16.dp),
                                    thickness = 0.6.dp,
                                    color = SurfaceDivider
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
