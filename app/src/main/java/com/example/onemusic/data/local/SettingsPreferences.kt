package com.example.onemusic.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class HapticIntensity(val title: String) {
    LIGHT("Nhẹ"),
    MEDIUM("Vừa"),
    STRONG("Mạnh")
}

@Serializable
enum class ThemeMode(val title: String) {
    LIGHT("Sáng"),
    DARK("Tối"),
    SYSTEM("Theo hệ thống")
}

@Serializable
data class AppSettings(
    // 1. Âm thanh & Phát nhạc (Audio & Playback)
    val isReplayGainEnabled: Boolean = true,
    val isHeadroomLimiterEnabled: Boolean = true,
    val isGaplessPlaybackEnabled: Boolean = true,
    val isCrossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 3,
    val pauseOnHeadsetDisconnect: Boolean = true,
    val resumeOnHeadsetConnect: Boolean = false,

    // 2. Giao diện & Hiển thị (Display & UI)
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val isDynamicMeshBackgroundEnabled: Boolean = true,
    val isMotionArtworkEnabled: Boolean = true,
    val isAutoMotionScanEnabled: Boolean = false,
    val isOnlineLyricsEnabled: Boolean = true,
    val isAutoDownloadArtistImagesEnabled: Boolean = true,
    val isKeepScreenOnEnabled: Boolean = false,
    val isHiResBadgeEnabled: Boolean = true,
    val isHideStatusBarEnabled: Boolean = false,
    val isHideNavigationBarEnabled: Boolean = false,

    // 3. Phản hồi xúc giác & Rung (Haptics)
    val isHapticFeedbackEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,

    // 4. Thư viện & Bộ nhớ (Library & Storage)
    val filterShortAudio: Boolean = true, // Filter audio < 30s
    val autoScanOnLaunch: Boolean = true,

    // 5. SoundAlive DSP & Bộ Chỉnh Âm (Equalizer)
    val soundAliveSettings: com.example.onemusic.data.model.SoundAliveSettings = com.example.onemusic.data.model.SoundAliveSettings(),

    // 6. Trạng thái phát nhạc cuối cùng (Last Playback State)
    val lastPlayedTrackId: String? = null,
    val lastPlayedPositionMs: Long = 0L,

    // 7. Chế độ xem Thư viện (Library View Mode: LIST, GRID_2; GRID_3 cũ được đổi thành GRID_2)
    val libraryViewMode: String = "LIST",

    // 8. Danh sách bài hát nghe gần đây (Recently Played)
    val recentlyPlayedTrackIds: List<String> = emptyList()
)

class SettingsPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("onemusic_settings_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        synchronized(lock) {
            if (_sharedSettingsFlow == null) {
                _sharedSettingsFlow = MutableStateFlow(loadSettings())
            }
        }
    }

    val settingsFlow: StateFlow<AppSettings>
        get() = _sharedSettingsFlow ?: MutableStateFlow(loadSettings()).also { _sharedSettingsFlow = it }

    fun getSettings(): AppSettings {
        return _sharedSettingsFlow?.value ?: loadSettings()
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        try {
            synchronized(lock) {
                val current = getSettings()
                val updated = transform(current)
                _sharedSettingsFlow?.value = updated
                saveSettings(updated)
            }
        } catch (_: Throwable) {}
    }

    fun saveSoundAliveSettings(soundAlive: com.example.onemusic.data.model.SoundAliveSettings) {
        updateSettings { it.copy(soundAliveSettings = soundAlive) }
    }

    fun getSoundAliveSettings(): com.example.onemusic.data.model.SoundAliveSettings {
        return getSettings().soundAliveSettings
    }

    fun saveLastPlaybackState(trackId: String?, positionMs: Long) {
        updateSettings {
            it.copy(
                lastPlayedTrackId = trackId,
                lastPlayedPositionMs = positionMs
            )
        }
    }

    fun addRecentlyPlayedTrack(trackId: String) {
        if (trackId.isBlank()) return
        updateSettings { current ->
            val updatedList = listOf(trackId) + current.recentlyPlayedTrackIds.filter { it != trackId }
            current.copy(recentlyPlayedTrackIds = updatedList.take(30))
        }
    }

    private fun loadSettings(): AppSettings {
        return try {
            val raw = prefs.getString(KEY_APP_SETTINGS, null) ?: return AppSettings()
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Throwable) {
            AppSettings()
        }
    }

    private fun saveSettings(settings: AppSettings) {
        try {
            val raw = json.encodeToString(settings)
            prefs.edit().putString(KEY_APP_SETTINGS, raw).apply()
        } catch (_: Throwable) {}
    }

    companion object {
        private const val KEY_APP_SETTINGS = "key_app_settings_v1"
        private val lock = Any()

        @Volatile
        private var _sharedSettingsFlow: MutableStateFlow<AppSettings>? = null

        @Volatile
        private var instance: SettingsPreferences? = null

        fun getInstance(context: Context): SettingsPreferences {
            return instance ?: synchronized(lock) {
                instance ?: SettingsPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
