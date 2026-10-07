package com.example.onemusic.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class DolbyAtmosMode(val displayName: String) {
    AUTO("Tự động"),
    MUSIC("Âm nhạc"),
    MOVIE("Phim ảnh"),
    VOICE("Giọng nói")
}

@Serializable
enum class SoundPreset(val displayName: String) {
    NORMAL("Chuẩn"),
    POP("Nhạc Pop"),
    CLASSIC("Cổ điển"),
    JAZZ("Nhạc Jazz"),
    ROCK("Nhạc Rock"),
    BASS_BOOST("Tăng âm trầm"),
    CLEAR_VOCAL("Giọng hát trong trẻo"),
    CUSTOM("Tùy chỉnh")
}

@Serializable
data class SoundAliveSettings(
    val isDolbyAtmosEnabled: Boolean = false,
    val dolbyMode: DolbyAtmosMode = DolbyAtmosMode.MUSIC,
    val isUhqUpscalerEnabled: Boolean = false,
    val preset: SoundPreset = SoundPreset.NORMAL,
    val bassBoostLevel: Int = 0, // 0 - 10 (Default: 0 = Pure bit-perfect disabled)
    val equalizerBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) // 9 bands (-10dB to +10dB)
)
