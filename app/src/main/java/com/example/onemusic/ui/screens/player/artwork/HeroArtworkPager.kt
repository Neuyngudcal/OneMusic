package com.example.onemusic.ui.screens.player.artwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextDisabled
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import kotlin.math.absoluteValue

/**
 * Pager ảnh bìa tràn viền của Now Playing (LAYER 1A): mỗi trang là một bài trong hàng đợi.
 * Trang đang phát (hoặc đang vuốt tới) của bài hiện tại hiện video bìa động nếu có, còn lại hiện ảnh tĩnh.
 * Nhấn giữ ảnh bìa → [onArtworkLongClick]. Chừa khoảng trống [controlsDeckHeight] ở đáy cho cụm điều khiển.
 */
@Composable
internal fun HeroArtworkPager(
    queue: List<Track>,
    track: Track?,
    pagerState: PagerState,
    motionVideoPath: String?,
    motionPlayer: androidx.media3.exoplayer.ExoPlayer?,
    hazeState: HazeState,
    isSheetFullyVisible: Boolean,
    scrimColor: Color,
    controlsDeckHeight: Dp,
    onArtworkLongClick: () -> Unit
) {
    val context = LocalContext.current
    val artworkScale = 1.0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SÂN KHẤU TRUNG TÂM (Tràn từ đỉnh máy y = 0 đến ngay trên cụm phím điều khiển)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1,
                key = { page -> if (queue.isNotEmpty() && page in queue.indices) "${queue[page].id}_$page" else "single_art" }
            ) { page ->
                val currentTrack = if (queue.isNotEmpty() && page in queue.indices) queue[page] else track

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                            this.alpha = lerp(0.35f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isCurrentOrTargetPage = page == pagerState.currentPage || page == pagerState.targetPage
                    val targetVideoPath = if (isCurrentOrTargetPage && currentTrack?.id == track?.id) motionVideoPath else null

                    // FULL BLEED HERO ARTWORK CONTAINER
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                            }
                            .apexBounceClick(
                                scaleDown = 0.98f,
                                enableHaptic = true,
                                onLongClick = {
                                    if (currentTrack != null) {
                                        onArtworkLongClick()
                                    }
                                }
                            )
                    ) {
                        if (targetVideoPath != null) {
                            MotionArtworkPlayer(
                                motionVideoPath = targetVideoPath,
                                motionPlayer = motionPlayer,
                                track = currentTrack,
                                hazeState = hazeState,
                                isSheetFullyVisible = isSheetFullyVisible,
                                scrimColor = scrimColor
                            )
                        } else if (currentTrack != null && currentTrack.artworkUrl.isNotBlank()) {
                            StaticAlbumArtwork(
                                context = context,
                                track = currentTrack,
                                applyMask = true,
                                scrimColor = scrimColor
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Khoảng đệm bảo lưu kích thước cụm phím điều khiển (Đồng bộ không gian chân trang với Layer 2)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(controlsDeckHeight)
        )
    }
}
