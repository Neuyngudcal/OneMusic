package com.example.onemusic.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.local.HapticIntensity
import com.example.onemusic.data.local.SettingsPreferences

/**
 * ApexHapticEngine: Advanced hardware tactile haptic feedback engine for OneMusic.
 * Uses Android 11+ (API 30+) Primitives (TICK, CLICK, LOW_TICK, QUICK_RISE) with safe amplitudes
 * and USAGE_ASSISTANCE_SONIFICATION to co-exist cleanly with music playback.
 */
class ApexHapticEngine private constructor(private val context: Context) {

    private val settingsPreferences = SettingsPreferences.getInstance(context)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * AudioAttributes to prevent audio ducking / muting during music streaming
     */
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()

    @delegate:androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val vibrationAttributes by lazy {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_TOUCH)
            .build()
    }

    /**
     * Core Multi-Tier Vibration Dispatcher tailored for high-end LRA (OnePlus Bionic, Pixel, Galaxy) and standard ERM
     */
    private fun dispatchHaptic(
        lightPrimitive: Int?,
        mediumPrimitive: Int?,
        strongPrimitive: Int?,
        baseDurationMs: Long,
        baseAmplitude: Int,
        scale: Float,
        fallbackView: View? = null
    ) {
        val settings = settingsPreferences.getSettings()
        if (!settings.isHapticFeedbackEnabled) return
        val currentVibrator = vibrator ?: return
        if (!currentVibrator.hasVibrator()) return

        val intensity = settings.hapticIntensity

        // 1. Tier 1: Hardware Primitives on Android 11+ (API 30+) - OnePlus 13 / OxygenOS 15 / Pixel / Samsung
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                when (intensity) {
                    HapticIntensity.LIGHT -> {
                        val prim = lightPrimitive ?: VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                        if (currentVibrator.areAllPrimitivesSupported(prim)) {
                            val composition = VibrationEffect.startComposition()
                                .addPrimitive(prim, (scale * 0.30f).coerceIn(0.05f, 0.40f))
                                .compose()
                            executeVibration(composition)
                            return
                        }
                    }
                    HapticIntensity.MEDIUM -> {
                        val prim = mediumPrimitive ?: VibrationEffect.Composition.PRIMITIVE_CLICK
                        if (currentVibrator.areAllPrimitivesSupported(prim)) {
                            val composition = VibrationEffect.startComposition()
                                .addPrimitive(prim, (scale * 0.85f).coerceIn(0.15f, 1.0f))
                                .compose()
                            executeVibration(composition)
                            return
                        }
                    }
                    HapticIntensity.STRONG -> {
                        val prim = strongPrimitive ?: VibrationEffect.Composition.PRIMITIVE_CLICK
                        if (currentVibrator.areAllPrimitivesSupported(prim)) {
                            val comp = VibrationEffect.startComposition()
                                .addPrimitive(prim, 1.0f)
                            if (currentVibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)) {
                                comp.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 1.0f, 12)
                            }
                            executeVibration(comp.compose())
                            return
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Tier 2: Modulated OneShot Waveform on Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (targetDuration, targetAmp) = when (intensity) {
                HapticIntensity.LIGHT -> Pair(
                    (baseDurationMs * 0.5f).toLong().coerceIn(6L, 14L),
                    (baseAmplitude * 0.35f * scale).toInt().coerceIn(20, 60)
                )
                HapticIntensity.MEDIUM -> Pair(
                    (baseDurationMs * 1.0f).toLong().coerceIn(14L, 28L),
                    (baseAmplitude * 1.0f * scale).toInt().coerceIn(80, 180)
                )
                HapticIntensity.STRONG -> Pair(
                    (baseDurationMs * 2.4f).toLong().coerceIn(38L, 90L),
                    255 // Full maximum hardware power
                )
            }

            val effect = if (currentVibrator.hasAmplitudeControl()) {
                VibrationEffect.createOneShot(targetDuration, targetAmp)
            } else {
                VibrationEffect.createOneShot(targetDuration, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            executeVibration(effect)
            return
        }

        // 3. Tier 3: Legacy Vibration or View Feedback
        try {
            @Suppress("DEPRECATION")
            val targetDuration = when (intensity) {
                HapticIntensity.LIGHT -> 8L
                HapticIntensity.MEDIUM -> 20L
                HapticIntensity.STRONG -> 50L
            }
            currentVibrator.vibrate(targetDuration)
        } catch (_: Exception) {
            fallbackView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // =========================================================================
    // TACTILE HAPTICS PRESETS
    // =========================================================================

    /**
     * 1. Physical Gear Tick: Micro-tick for Seekbar, volume knob, and continuous scrubbing.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun performGearTick(scale: Float = 0.18f, fallbackView: View? = null) {
        val (lightPrim, medPrim, strongPrim) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Triple(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        } else Triple(null, null, null)

        dispatchHaptic(
            lightPrimitive = lightPrim,
            mediumPrimitive = medPrim,
            strongPrimitive = strongPrim,
            baseDurationMs = 8L,
            baseAmplitude = 70,
            scale = scale,
            fallbackView = fallbackView
        )
    }

    /**
     * 2. Crisp Light Tap: Standard button taps, filter chips, navigation switches.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun performCrispTap(scale: Float = 0.35f, fallbackView: View? = null) {
        val (lightPrim, medPrim, strongPrim) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Triple(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        } else Triple(null, null, null)

        dispatchHaptic(
            lightPrimitive = lightPrim,
            mediumPrimitive = medPrim,
            strongPrimitive = strongPrim,
            baseDurationMs = 15L,
            baseAmplitude = 140,
            scale = scale,
            fallbackView = fallbackView
        )
    }

    /**
     * 3. Item Lift / Long-press: Smooth ramp-up pulse when lifting an item or opening dialog.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun performItemLift(scale: Float = 0.65f, fallbackView: View? = null) {
        val (lightPrim, medPrim, strongPrim) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Triple(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
            )
        } else Triple(null, null, null)

        dispatchHaptic(
            lightPrimitive = lightPrim,
            mediumPrimitive = medPrim,
            strongPrimitive = strongPrim,
            baseDurationMs = 30L,
            baseAmplitude = 230,
            scale = scale,
            fallbackView = fallbackView
        )
    }

    /**
     * 4. Confirmation Thud: Play/Pause state change, favorite heart toggle.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun performConfirmation(scale: Float = 0.50f, fallbackView: View? = null) {
        val (lightPrim, medPrim, strongPrim) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Triple(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        } else Triple(null, null, null)

        dispatchHaptic(
            lightPrimitive = lightPrim,
            mediumPrimitive = medPrim,
            strongPrimitive = strongPrim,
            baseDurationMs = 22L,
            baseAmplitude = 190,
            scale = scale,
            fallbackView = fallbackView
        )
    }

    /**
     * 5. Spring Latch: Micro tactile lock when crossing swipe threshold.
     */
    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun performSpringLatch(scale: Float = 0.55f, fallbackView: View? = null) {
        val (lightPrim, medPrim, strongPrim) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Triple(
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
            )
        } else Triple(null, null, null)

        dispatchHaptic(
            lightPrimitive = lightPrim,
            mediumPrimitive = medPrim,
            strongPrimitive = strongPrim,
            baseDurationMs = 26L,
            baseAmplitude = 210,
            scale = scale,
            fallbackView = fallbackView
        )
    }

    // =========================================================================
    // EXECUTION PIPELINE
    // =========================================================================

    private fun executeVibration(effect: VibrationEffect) {
        try {
            val currentVibrator = vibrator ?: return
            if (!currentVibrator.hasVibrator()) return
            if (!settingsPreferences.getSettings().isHapticFeedbackEnabled) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currentVibrator.vibrate(effect, vibrationAttributes)
            } else {
                @Suppress("DEPRECATION")
                currentVibrator.vibrate(effect, audioAttributes)
            }
        } catch (_: Throwable) {
            // Missing android.permission.VIBRATE fallback or driver safety
        }
    }

    companion object {
        @Volatile
        private var instance: ApexHapticEngine? = null

        fun from(context: Context): ApexHapticEngine {
            return instance ?: synchronized(this) {
                instance ?: ApexHapticEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * CompositionLocal & Hook helpers for Jetpack Compose
 */
val LocalApexHaptics: ProvidableCompositionLocal<ApexHapticEngine> = staticCompositionLocalOf {
    error("ApexHapticEngine has not been provided in CompositionLocalProvider")
}

@Composable
fun rememberApexHaptics(): ApexHapticEngine {
    val context = LocalContext.current
    return remember(context) { ApexHapticEngine.from(context) }
}
