package com.example.onemusic.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.ui.components.NowBar
import com.example.onemusic.ui.navigation.ApexBottomNavigation
import com.example.onemusic.ui.navigation.Screen
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.Dp

/**
 * Cụm nổi ở đáy màn hình: chỉ báo đang quét, NowBar, thanh tab, và Snackbar dùng chung ngay phía trên.
 * Tự ẩn khi mở bàn phím hoặc màn toàn màn hình (Thư mục nhạc, Dọn trùng lặp). Báo chiều cao thật qua [onHeightMeasured].
 */
@Composable
internal fun BoxScope.BottomControlsOverlay(
    isFullScreenOverlayVisible: Boolean,
    isScanning: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    tracks: List<Track>,
    playerController: MusicPlayerController,
    onExpandPlayer: () -> Unit,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onHeightMeasured: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    bottomOverlayPadding: Dp
) {
    // Floating Bottom Controls: NowBar & Bottom Navigation Dock (Automatically hides when keyboard is open)
    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    AnimatedVisibility(
        visible = !isFullScreenOverlayVisible && !isKeyboardOpen,
        enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
        ),
        exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
        ),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { if (it.height > 0) onHeightMeasured(it.height) }
        ) {
            // Chỉ báo quét nhạc: không chặn thao tác, hiện ở mọi tab
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(PillShape)
                        .background(SurfaceElevated.copy(alpha = 0.92f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = Brand, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Đang quét thư viện nhạc…", color = TextPrimary, fontSize = 13.sp)
                }
            }

            // Floating Mini Player (Now Bar)
            AnimatedVisibility(
                visible = currentTrack != null,
                enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(200)),
                exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + shrinkVertically(tween(200))
            ) {
                NowBar(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    onBarClick = {
                        if (currentTrack != null) {
                            onExpandPlayer()
                        } else if (tracks.isNotEmpty()) {
                            playerController.setQueue(tracks, startIndex = 0, autoPlay = false)
                            onExpandPlayer()
                        }
                    },
                    onPlayPauseClick = {
                        if (currentTrack != null) {
                            playerController.togglePlayPause()
                        } else if (tracks.isNotEmpty()) {
                            playerController.setQueue(tracks, startIndex = 0, autoPlay = true)
                        }
                    },
                    onNextClick = { playerController.skipToNext() },
                    onPreviousClick = { playerController.skipToPrevious() }
                )
            }

            // Floating Bottom Navigation Dock (4 Tabs Icon-Only)
            ApexBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = onNavigate,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )
        }
    }

    // Snackbar dùng chung: nằm ngay trên cụm NowBar + thanh tab để không bị che
    androidx.compose.material3.SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomOverlayPadding)
    ) { data ->
        androidx.compose.material3.Snackbar(
            snackbarData = data,
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            containerColor = com.example.onemusic.theme.SurfaceElevated,
            contentColor = TextPrimary,
            actionColor = com.example.onemusic.theme.Brand
        )
    }
}
