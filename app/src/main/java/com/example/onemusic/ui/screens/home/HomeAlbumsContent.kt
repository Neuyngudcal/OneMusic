package com.example.onemusic.ui.screens.home

import com.example.onemusic.data.search.LibraryGrouping
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.ui.components.ApexDropdownMenu
import com.example.onemusic.ui.components.ApexDropdownMenuItem
import com.example.onemusic.ui.components.ApexDropdownDivider
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewModule
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
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary
import com.example.onemusic.theme.apexGroupedCardItem

/** HomeSubView.ALBUMS: danh sách album dạng danh sách / lưới 2 cột / lưới 3 cột. Chế độ xem và trạng thái menu do màn cha giữ. */
@Composable
internal fun HomeAlbumsContent(
    tracks: List<Track>,
    albumViewMode: AlbumViewMode,
    onAlbumViewModeChange: (AlbumViewMode) -> Unit,
    isViewMenuExpanded: Boolean,
    onViewMenuExpandedChange: (Boolean) -> Unit,
    hazeState: HazeState,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: (List<Track>) -> Unit
) {
    // Gộp album bằng hàm dùng chung: theo (tên album, nghệ sĩ chính), sắp xếp A→Z
    val rawAlbums = remember(tracks) {
        LibraryGrouping.groupAlbums(tracks).map { album ->
            AlbumItemData(
                key = album.key,
                name = album.name,
                artist = album.artist,
                trackCount = album.tracks.size,
                artworkUrl = album.tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
                tracks = album.tracks
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
        ) {
            // 1. HEADER (Back, Title, Count, Layout Mode Switcher Button)
            item(key = "albums_header") {
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
                            text = "Album",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "${rawAlbums.size} album • ${tracks.size} bài hát",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.5.sp
                            )
                        )
                    }

                    // Circular Layout Switcher Button
                    val currentModeIcon = when (albumViewMode) {
                        AlbumViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
                        AlbumViewMode.GRID_2 -> Icons.Rounded.GridView
                        AlbumViewMode.GRID_3 -> Icons.Rounded.ViewModule
                    }

                    Box {
                        ApexCircularGlassButton(
                            icon = currentModeIcon,
                            contentDescription = "Tùy chọn hiển thị",
                            onClick = { onViewMenuExpandedChange(!isViewMenuExpanded) },
                            size = 44.dp,
                            iconSize = 22.dp
                        )

                        ApexDropdownMenu(
                            expanded = isViewMenuExpanded,
                            onDismissRequest = { onViewMenuExpandedChange(false) },
                            hazeState = hazeState,
                            width = 205.dp
                        ) {
                            ApexDropdownMenuItem(
                                text = "Danh sách",
                                icon = Icons.AutoMirrored.Rounded.ViewList,
                                trailingText = if (albumViewMode == AlbumViewMode.LIST) "✓" else null,
                                trailingColor = Brand,
                                textColor = if (albumViewMode == AlbumViewMode.LIST) Brand else TextPrimary,
                                onClick = {
                                    onAlbumViewModeChange(AlbumViewMode.LIST)
                                    onViewMenuExpandedChange(false)
                                }
                            )
                            ApexDropdownDivider()
                            ApexDropdownMenuItem(
                                text = "Lưới 2 cột",
                                icon = Icons.Rounded.GridView,
                                trailingText = if (albumViewMode == AlbumViewMode.GRID_2) "✓" else null,
                                trailingColor = Brand,
                                textColor = if (albumViewMode == AlbumViewMode.GRID_2) Brand else TextPrimary,
                                onClick = {
                                    onAlbumViewModeChange(AlbumViewMode.GRID_2)
                                    onViewMenuExpandedChange(false)
                                }
                            )
                            ApexDropdownDivider()
                            ApexDropdownMenuItem(
                                text = "Lưới 3 cột",
                                icon = Icons.Rounded.ViewModule,
                                trailingText = if (albumViewMode == AlbumViewMode.GRID_3) "✓" else null,
                                trailingColor = Brand,
                                textColor = if (albumViewMode == AlbumViewMode.GRID_3) Brand else TextPrimary,
                                onClick = {
                                    onAlbumViewModeChange(AlbumViewMode.GRID_3)
                                    onViewMenuExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }


            // 2. ALBUMS CONTENT BY VIEW MODE
            if (rawAlbums.isEmpty()) {
                item(key = "albums_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = IvoryMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Không có album nào trong thư viện",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 14.5.sp
                                )
                            )
                        }
                    }
                }
            } else {
                when (albumViewMode) {
                    AlbumViewMode.LIST -> {
                        val total = rawAlbums.size
                        itemsIndexed(rawAlbums, key = { index, it -> "list_${it.name}_$index" }) { index, album ->
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
                                                onOpenAlbum(album.key)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Artwork
                                        if (!album.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = album.artworkUrl,
                                                contentDescription = album.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(SurfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Album,
                                                    contentDescription = null,
                                                    tint = IvoryFaint,
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.name,
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
                                                text = "${album.artist} • ${album.trackCount} bài hát",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = TextSecondary,
                                                    fontSize = 13.5.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Quick play button
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryIvory)
                                                .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                                    onPlayTracks(album.tracks)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Phát nhanh",
                                                tint = CharcoalBlack,
                                                modifier = Modifier.size(20.dp)
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

                    AlbumViewMode.GRID_2 -> {
                        val pairs = rawAlbums.chunked(2)
                        itemsIndexed(pairs, key = { idx, pair -> "g2_${idx}_" + pair.joinToString("_") { it.name } }) { _, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                for (album in pair) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                onOpenAlbum(album.key)
                                            }
                                    ) {
                                        // Artwork Frame
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceActiveIndicator)
                                        ) {
                                            if (!album.artworkUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = album.artworkUrl,
                                                    contentDescription = album.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(SurfaceElevated),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Album,
                                                        contentDescription = null,
                                                        tint = IvoryDisabled,
                                                        modifier = Modifier.size(52.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Album Name
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 15.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Artist Name
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    AlbumViewMode.GRID_3 -> {
                        val triplets = rawAlbums.chunked(3)
                        itemsIndexed(triplets, key = { idx, triplet -> "g3_${idx}_" + triplet.joinToString("_") { it.name } }) { _, triplet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (album in triplet) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                                onOpenAlbum(album.key)
                                            }
                                    ) {
                                        // Artwork Frame
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SurfaceActiveIndicator)
                                        ) {
                                            if (!album.artworkUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = album.artworkUrl,
                                                    contentDescription = album.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(SurfaceElevated),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Album,
                                                        contentDescription = null,
                                                        tint = IvoryDisabled,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Album Name
                                        Text(
                                            text = album.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(1.dp))

                                        // Artist Name
                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                val emptySlots = 3 - triplet.size
                                for (i in 0 until emptySlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
