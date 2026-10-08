package com.example.onemusic.ui.navigation

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.LocalHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.onemusic.theme.ApexGlassSurfaceBg
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.apexFrostedGlass

enum class Screen(
    val title: String,
    val icon: ImageVector
) {
    HOME("Trang chủ", Icons.Rounded.Home),
    LIBRARY("Thư viện", Icons.Rounded.LibraryMusic),
    SEARCH("Tìm kiếm", Icons.Rounded.Search),
    SETTINGS("Cài đặt", Icons.Rounded.Settings)
}

/**
 * OneMusic Apex Prism Floating Bottom Navigation Dock (4 Tabs - Icons Only)
 *
 * Architecture & Features:
 * - 4 Minimalist Tabs: Home -> Library -> Search -> Settings (22dp..24dp 100% Centered Icons)
 * - Dual-Touch Interaction: Single Tap + Continuous 1:1 Drag-to-Snap
 * - Elastic Border Resistance & Real-time Slot Snapping Haptic Ticks
 * - Real-Time GPU Frosted Glass Surface (ApexGlassSurfaceBg with 24dp blur)
 */
@Composable
fun ApexBottomNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs = remember { Screen.entries }
    val tabCount = tabs.size
    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomMargin = navBarBottom + 12.dp

    val hazeState = LocalHazeState.current

    // Target index based on currentScreen enum ordinal (0..3)
    val currentTargetIndex = currentScreen.ordinal
    val currentScreenState by androidx.compose.runtime.rememberUpdatedState(currentScreen)
    val onScreenSelectedState by androidx.compose.runtime.rememberUpdatedState(onScreenSelected)

    // Animatable fraction representing active pill position (0f..3f)
    val fractionAnim = remember { Animatable(currentTargetIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(currentTargetIndex.toFloat()) }
    var lastSnappedSlot by remember { mutableIntStateOf(currentTargetIndex) }

    // Sync state when external navigation changes currentScreen
    LaunchedEffect(currentScreen) {
        if (!isDragging && fractionAnim.targetValue != currentScreen.ordinal.toFloat()) {
            fractionAnim.animateTo(
                targetValue = currentScreen.ordinal.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.76f,
                    stiffness = 400f
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = bottomMargin),
        contentAlignment = Alignment.Center
    ) {
        // Floating 4-Tab Glass Dock Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = PillShape,
                    ambientColor = AppTheme.colors.shadow,
                    spotColor = AppTheme.colors.shadow
                )
                .clip(PillShape)
                .apexFrostedGlass(
                    backgroundColor = AppTheme.colors.surface1.copy(alpha = 0.88f),
                    blurRadius = 20.dp,
                    hazeState = hazeState
                )
                .border(0.85.dp, AppTheme.colors.pillBorderBrush, PillShape)
                .padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val dockWidthPx = with(density) { maxWidth.toPx() }
            val tabWidth = maxWidth / tabCount
            val tabWidthPx = dockWidthPx / tabCount

            // Unified Pointer Gesture Engine: Tap + Continuous Drag (100% Absolute Coordinate Mapping)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tabCount) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val tabW = (size.width.toFloat() / tabCount).coerceAtLeast(1f)
                            val initialSlot = (down.position.x / tabW).toInt().coerceIn(0, tabCount - 1)

                            var currentX = down.position.x
                            var isDrag = false
                            val startX = down.position.x

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                currentX = change.position.x
                                val delta = currentX - startX
                                if (!isDrag && kotlin.math.abs(delta) > 12f) {
                                    isDrag = true
                                    isDragging = true
                                }
                                if (isDrag) {
                                    change.consume()
                                    // Finger centers directly on the active pill
                                    val rawFrac = (currentX - (tabW / 2f)) / tabW
                                    val maxFrac = (tabCount - 1).toFloat()
                                    val clamped = when {
                                        rawFrac < 0f -> rawFrac * 0.25f
                                        rawFrac > maxFrac -> maxFrac + (rawFrac - maxFrac) * 0.25f
                                        else -> rawFrac
                                    }
                                    dragFraction = clamped.coerceIn(-0.35f, maxFrac + 0.35f)

                                    val slot = (currentX / tabW).toInt().coerceIn(0, tabCount - 1)
                                    if (slot != lastSnappedSlot) {
                                        lastSnappedSlot = slot
                                        try {
                                            hapticEngine.performCrispTap(scale = 0.20f, fallbackView = currentView)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }

                            val finalSlot = (currentX / tabW).toInt().coerceIn(0, tabCount - 1)
                            val targetScreen = tabs[finalSlot]
                            isDragging = false

                            coroutineScope.launch {
                                if (isDrag) {
                                    fractionAnim.snapTo(dragFraction)
                                }
                                fractionAnim.animateTo(
                                    targetValue = finalSlot.toFloat(),
                                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 400f)
                                )
                            }

                            if (targetScreen != currentScreenState) {
                                try {
                                    hapticEngine.performCrispTap(scale = 0.35f, fallbackView = currentView)
                                } catch (_: Exception) {}
                                onScreenSelectedState(targetScreen)
                            }
                        }
                    }
            ) {
                // Movement dynamics for physical squash & stretch
                val currentFraction = if (isDragging) dragFraction else fractionAnim.value
                val movementDistance = (currentFraction - currentFraction.toInt()).let { if (it < 0.5f) it else 1f - it } * 2f
                val stretchX = if (isDragging) 1.08f else (1f + 0.12f * movementDistance)
                val squashY = if (isDragging) 0.92f else (1f - 0.06f * movementDistance)

                // 1. Sliding Elastic Active Pill Indicator (Rendered via GPU Layer Translation - 0ms Recomposition)
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = currentFraction * tabWidthPx
                            scaleX = stretchX
                            scaleY = squashY
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .clip(PillShape)
                        .background(AppTheme.colors.surfaceActiveIndicator)
                )

                // 2. Interactive Icons-Only Tab Items
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { screen ->
                        val isSelected = screen == currentScreen
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                // Cử chỉ chạm/kéo do pointerInput tự xử lý nên TalkBack không biết đây là tab.
                                // Khai báo semantics (KHÔNG dùng clickable – sẽ tranh cử chỉ kéo) để đọc
                                // "Trang chủ, Tab, Đã chọn" và nhấn đúp chuyển tab được.
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    selected = isSelected
                                    contentDescription = screen.title
                                    onClick(label = screen.title) {
                                        if (screen != currentScreenState) onScreenSelectedState(screen)
                                        true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = null,
                                tint = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
