package com.example.onemusic.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUrl: String,
    val artworkUrl: String,
    val isHiRes: Boolean = false,
    val isDolbyAtmos: Boolean = false,
    val bitRate: String = "320 kbps",
    val lyrics: List<LyricLine> = emptyList(),
    val isFavorite: Boolean = false,
    val replayGainDb: Float? = null,
    val peakLevel: Float? = null,
    val replayGainOrigin: String? = null,
    val motionArtworkUrl: String? = null,
    val hasMotionArtwork: Boolean? = null
)
