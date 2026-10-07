package com.example.onemusic.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

@Immutable
@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val isEnhanced: Boolean = false,
    val isSynced: Boolean = true
)

