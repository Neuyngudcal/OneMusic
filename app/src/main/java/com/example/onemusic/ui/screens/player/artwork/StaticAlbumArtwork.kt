package com.example.onemusic.ui.screens.player.artwork

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextDisabled

/**
 * Static album artwork fallback composable.
 */
@Composable
internal fun StaticAlbumArtwork(
    context: Context,
    track: Track?,
    applyMask: Boolean = true,
    scrimColor: Color = Color.Transparent
) {
    if (track != null && track.artworkUrl.isNotBlank()) {
        val imageRequest = remember(track.artworkUrl) {
            ImageRequest.Builder(context)
                .data(track.artworkUrl)
                .crossfade(150)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }
        val maskModifier = if (applyMask) {
            Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val targetH = size.height
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.28f to Color.Black,
                            0.42f to Color.Black.copy(alpha = 0.96f),
                            0.54f to Color.Black.copy(alpha = 0.88f),
                            0.66f to Color.Black.copy(alpha = 0.74f),
                            0.76f to Color.Black.copy(alpha = 0.55f),
                            0.85f to Color.Black.copy(alpha = 0.35f),
                            0.92f to Color.Black.copy(alpha = 0.18f),
                            0.97f to Color.Black.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            startY = 0f,
                            endY = targetH
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(maskModifier)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )

            // Lớp sương mù hòa sắc (Palette-Tinted Scrim Fog) nhuộm nhẹ chân ảnh hòa tan vào Ambient Mesh
            if (applyMask && scrimColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.30f to Color.Transparent,
                                0.52f to scrimColor.copy(alpha = 0.20f),
                                0.70f to scrimColor.copy(alpha = 0.55f),
                                0.86f to scrimColor.copy(alpha = 0.85f),
                                1.00f to scrimColor
                            )
                        )
                )
            }
        }
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
