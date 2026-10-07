package com.example.onemusic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for caching Apple Music animated/motion album artwork metadata.
 *
 * - Positive cache (hasMotion = true): Stores m3u8 URL + local video path. Persisted indefinitely.
 * - Negative cache (hasMotion = false): Album confirmed to have no motion art. Expires after 30 days.
 * - Unchecked (hasMotion = null equivalent → not in DB): Track has not been queried yet.
 */
@Entity(tableName = "motion_artwork")
data class MotionArtworkEntity(
    @PrimaryKey val trackId: String,
    val appleAlbumId: String = "",
    val albumName: String = "",
    val artistName: String = "",
    val motionSquareUrl: String = "",
    val motionTallUrl: String = "",
    val previewFrameUrl: String = "",
    val localVideoPath: String = "",
    val hasMotion: Boolean = false,
    val isDownloaded: Boolean = false,
    val cachedAt: Long = 0L
)
