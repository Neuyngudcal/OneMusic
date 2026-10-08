package com.example.onemusic.ui.screens.library.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Floating Bottom Pill Bar for Multi-Select batch actions (Thumb zone).
 */
@Composable
fun LibraryBatchActionBar(
    isVisible: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onPlaySelected: () -> Unit,
    onPlayNextSelected: (() -> Unit)?,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = LocalBottomOverlayPadding.current, start = 20.dp, end = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(18.dp, PillShape, ambientColor = AppTheme.colors.shadow)
                .clip(PillShape)
                .background(AppTheme.colors.surface1.copy(alpha = 0.95f))
                .border(0.85.dp, AppTheme.colors.stroke, PillShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Counter & Close button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .clip(CircleShape)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hủy",
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$selectedCount đã chọn",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                // Actions: Select All / Deselect, Play Selected, Play Next, Add to Playlist
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Select All / Deselect Toggle
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(AppTheme.colors.subtle)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onToggleSelectAll()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (selectedCount == totalCount && totalCount > 0) "Bỏ chọn" else "Tất cả",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    if (selectedCount > 0) {
                        // Play Selected
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(AppTheme.colors.textPrimary)
                                .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                    onPlaySelected()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = AppTheme.colors.onInverse,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Phát",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AppTheme.colors.onInverse,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Play Next
                        if (onPlayNextSelected != null) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.stroke)
                                    .apexBounceClick(scaleDown = 0.90f, enableHaptic = true) {
                                        onPlayNextSelected()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Phát kế tiếp",
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Add to Playlist
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppTheme.colors.stroke)
                                .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                    onAddToPlaylist()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = "Thêm vào playlist",
                                tint = AppTheme.colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
