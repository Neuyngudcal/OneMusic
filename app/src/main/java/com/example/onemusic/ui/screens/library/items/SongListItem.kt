package com.example.onemusic.ui.screens.library.items

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.apexGroupedCardItem
import com.example.onemusic.ui.components.ApexHiResBadge
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration

@Composable
fun SongListItem(
    track: Track,
    isCurrent: Boolean,
    isMultiSelectMode: Boolean,
    isSelectedInBatch: Boolean,
    isHiResBadgeEnabled: Boolean,
    index: Int,
    total: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .apexGroupedCardItem(index = index, total = total, cornerRadius = 26.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .apexBounceClick(
                        scaleDown = 0.98f,
                        enableHaptic = true,
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi-Select Checkbox Circle
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelectedInBatch) AppTheme.colors.textPrimary else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelectedInBatch) AppTheme.colors.textPrimary else IvoryDisabled,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelectedInBatch) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Đã chọn",
                                tint = AppTheme.colors.onInverse,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Artwork or fallback music note icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colors.surfaceActiveIndicator),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) AppTheme.colors.accent else AppTheme.colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isHiResBadgeEnabled && track.isHiRes) {
                            ApexHiResBadge()
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${track.artist} • ${formatDuration(track.durationMs, padMinutes = true)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                                onToggleFavorite()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                            tint = if (track.isFavorite) ApexRose else AppTheme.colors.textPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (index < total - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    thickness = 0.6.dp,
                    color = AppTheme.colors.divider
                )
            }
        }
    }
}
