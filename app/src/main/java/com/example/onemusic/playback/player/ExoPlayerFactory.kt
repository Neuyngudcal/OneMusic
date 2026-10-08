package com.example.onemusic.playback.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import com.example.onemusic.playback.HeadroomLimiterAudioProcessor

/** Dựng ExoPlayer phát nhạc chính: AudioSink có ReplayGain + Headroom Limiter, tua chính xác, bộ đệm, audio focus. */
internal object ExoPlayerFactory {

    fun createMusicPlayer(context: Context, headroomLimiter: HeadroomLimiterAudioProcessor): ExoPlayer {
        // 1 & 2. Audiophile AudioSink with dynamic ReplayGain & Headroom Soft-Knee Limiter
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(headroomLimiter))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        // 1. Accurate MP3, AAC, and FLAC Frame Index Seeking
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(
                Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING or Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING
            )
            .setFlacExtractorFlags(
                FlacExtractor.FLAG_DISABLE_ID3_METADATA
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        // 2. High-Performance Local Audio Load Control (Fast seek & responsive buffering)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 3_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 3. Professional Media Audio Attributes & Audio Focus Handling
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
            }
    }
}
