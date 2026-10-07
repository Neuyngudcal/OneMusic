package com.example.onemusic.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import com.example.onemusic.data.model.DolbyAtmosMode
import com.example.onemusic.data.model.SoundAliveSettings
import com.example.onemusic.data.model.SoundPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AudioEffectManager: Defensive, Hardware-Hardened DSP Audio Effect Engine.
 * - Multi-layer try/catch protecting against OEM-specific AudioFx hardware crashes
 *   (UnsupportedOperationException, IllegalStateException, IllegalArgumentException, SecurityException).
 * - Safe release and state reconciliation on audio session transitions.
 */
class AudioEffectManager(
    private val settingsPreferences: com.example.onemusic.data.local.SettingsPreferences? = null
) {

    companion object {
        private const val TAG = "AudioEffectManager"
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    private val _settings = MutableStateFlow(
        settingsPreferences?.getSoundAliveSettings() ?: SoundAliveSettings()
    )
    val settings: StateFlow<SoundAliveSettings> = _settings.asStateFlow()

    init {
        // Initial setup from persisted settings
        settingsPreferences?.let { prefs ->
            _settings.value = prefs.getSoundAliveSettings()
        }
    }

    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        
        // Clean up previous effects before attaching to new session
        safeReleaseEffects()
        currentSessionId = audioSessionId
        
        try {
            applySettings(_settings.value)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize DSP effects on session " + audioSessionId + ": " + t.message)
        }
    }

    fun updateDolbyAtmos(enabled: Boolean, mode: DolbyAtmosMode = _settings.value.dolbyMode) {
        val updated = _settings.value.copy(
            isDolbyAtmosEnabled = enabled,
            dolbyMode = mode
        )
        _settings.value = updated
        settingsPreferences?.saveSoundAliveSettings(updated)
    }

    fun updateUhqUpscaler(enabled: Boolean) {
        val updated = _settings.value.copy(isUhqUpscalerEnabled = enabled)
        _settings.value = updated
        settingsPreferences?.saveSoundAliveSettings(updated)
    }

    @Synchronized
    fun updateBassBoost(level: Int) {
        val clamped = level.coerceIn(0, 10)
        val updated = _settings.value.copy(bassBoostLevel = clamped)
        _settings.value = updated
        settingsPreferences?.saveSoundAliveSettings(updated)
        
        if (currentSessionId <= 0) return
        
        if (clamped > 0) {
            try {
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, currentSessionId)
                }
                bassBoost?.apply {
                    if (strengthSupported) {
                        setStrength((clamped * 100).toShort())
                    }
                    enabled = true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "BassBoost hardware error: " + t.message)
                safeReleaseBassBoost()
            }
        } else {
            try {
                bassBoost?.enabled = false
            } catch (t: Throwable) {
                safeReleaseBassBoost()
            }
        }
    }

    @Synchronized
    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) {
        val bands = _settings.value.equalizerBands.toMutableList()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = gainDb.coerceIn(-10f, 10f)
            val updated = _settings.value.copy(
                equalizerBands = bands,
                preset = SoundPreset.CUSTOM
            )
            _settings.value = updated
            settingsPreferences?.saveSoundAliveSettings(updated)
            applySettings(updated)
        }
    }

    @Synchronized
    fun applyPreset(preset: SoundPreset) {
        val gains = when (preset) {
            SoundPreset.NORMAL -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            SoundPreset.POP -> listOf(1f, 2f, 3f, 1f, -1f, -1f, 1f, 2f, 3f)
            SoundPreset.CLASSIC -> listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f)
            SoundPreset.JAZZ -> listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f)
            SoundPreset.ROCK -> listOf(5f, 3f, 1f, 0f, -1f, 1f, 3f, 4f, 5f)
            SoundPreset.BASS_BOOST -> listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f)
            SoundPreset.CLEAR_VOCAL -> listOf(-2f, -1f, 0f, 2f, 4f, 5f, 4f, 2f, 1f)
            SoundPreset.CUSTOM -> _settings.value.equalizerBands
        }
        val updated = _settings.value.copy(
            preset = preset,
            equalizerBands = gains
        )
        _settings.value = updated
        settingsPreferences?.saveSoundAliveSettings(updated)
        applySettings(updated)
    }

    @Synchronized
    fun resetToFlat() {
        val flat = SoundAliveSettings(
            isDolbyAtmosEnabled = false,
            dolbyMode = DolbyAtmosMode.MUSIC,
            isUhqUpscalerEnabled = false,
            preset = SoundPreset.NORMAL,
            bassBoostLevel = 0,
            equalizerBands = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )
        _settings.value = flat
        settingsPreferences?.saveSoundAliveSettings(flat)
        applySettings(flat)
    }

    @Synchronized
    private fun applySettings(s: SoundAliveSettings) {
        if (currentSessionId <= 0) return

        val isEqActive = s.preset != SoundPreset.NORMAL || s.equalizerBands.any { it != 0f }
        val isBassActive = s.bassBoostLevel > 0

        // 1. Equalizer: Only activate hardware DSP if bands are modified
        try {
            if (isEqActive) {
                if (equalizer == null) {
                    equalizer = Equalizer(0, currentSessionId)
                }
                equalizer?.let { eq ->
                    eq.enabled = true
                    s.equalizerBands.forEachIndexed { index, gain ->
                        applyBandGain(index, gain)
                    }
                }
            } else {
                try {
                    equalizer?.enabled = false
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Equalizer DSP error: " + t.message)
            safeReleaseEqualizer()
        }

        // 2. BassBoost: Only activate if bass boost level > 0
        try {
            if (isBassActive) {
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, currentSessionId)
                }
                bassBoost?.apply {
                    if (strengthSupported) {
                        setStrength((s.bassBoostLevel * 100).toShort())
                    }
                    enabled = true
                }
            } else {
                try {
                    bassBoost?.enabled = false
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "BassBoost DSP error: " + t.message)
            safeReleaseBassBoost()
        }
    }

    private fun applyBandGain(bandIndex: Int, gainDb: Float) {
        try {
            val eq = equalizer ?: return
            if (bandIndex < eq.numberOfBands) {
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val level = ((gainDb / 10f) * maxLevel).toInt().toShort().coerceIn(minLevel, maxLevel)
                eq.setBandLevel(bandIndex.toShort(), level)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error setting band " + bandIndex + ": " + t.message)
        }
    }

    private fun safeReleaseEqualizer() {
        try {
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null
    }

    private fun safeReleaseBassBoost() {
        try {
            bassBoost?.release()
        } catch (_: Throwable) {}
        bassBoost = null
    }

    @Synchronized
    private fun safeReleaseEffects() {
        safeReleaseEqualizer()
        safeReleaseBassBoost()
    }

    @Synchronized
    fun release() {
        safeReleaseEffects()
        currentSessionId = 0
    }
}
