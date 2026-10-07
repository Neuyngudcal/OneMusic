package com.example.onemusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HeadroomLimiterAudioProcessorTest {

    @Test
    fun testPcm16HeadroomAndLimiter() {
        val processor = HeadroomLimiterAudioProcessor(defaultGainDb = -3.5f)
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outFormat = processor.configure(format)
        processor.flush()

        assertEquals(C.ENCODING_PCM_16BIT, outFormat.encoding)

        // Create buffer with extreme maximum values (clipping samples)
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        input.putShort(32767.toShort())  // Max positive
        input.putShort((-32768).toShort()) // Max negative
        input.putShort(16384.toShort())
        input.putShort(0.toShort())
        input.flip()

        processor.queueInput(input)
        val output = processor.output

        assertTrue(output.hasRemaining())
        val out1 = output.short
        val out2 = output.short
        val out3 = output.short
        val out4 = output.short

        // Check that pre-amp attenuated maximum peak below 32767
        assertTrue("Output should be attenuated below max short", out1 < 30000 && out1 > 0)
        assertTrue("Output should be attenuated above min short", out2 > -30000 && out2 < 0)
        assertEquals(0.toShort(), out4)
    }

    @Test
    fun testPcmFloatHeadroomAndLimiter() {
        val processor = HeadroomLimiterAudioProcessor(defaultGainDb = -3.5f)
        val format = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        val outFormat = processor.configure(format)
        processor.flush()

        assertEquals(C.ENCODING_PCM_FLOAT, outFormat.encoding)

        // Create buffer with True Peak +3.5 dB (+1.5f float)
        val input = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
        input.putFloat(1.5f) // True peak exceeding 1.0f
        input.putFloat(-1.5f)
        input.putFloat(0.5f)
        input.putFloat(0.0f)
        input.flip()

        processor.queueInput(input)
        val output = processor.output

        assertTrue(output.hasRemaining())
        val f1 = output.float
        val f2 = output.float
        val f3 = output.float
        val f4 = output.float

        // Verify that even a +1.5f (+3.5 dBTP) True Peak is tamed safely at or below 1.0f!
        assertTrue("Peak exceeding 1.0f must be compressed below or equal to 1.0f", f1 <= 1.0f && f1 > 0.0f)
        assertTrue("Peak exceeding -1.0f must be compressed above or equal to -1.0f", f2 >= -1.0f && f2 < 0.0f)
        assertEquals(0.0f, f4, 0.0001f)
    }

    @Test
    fun testDynamicReplayGainAdjustment() {
        val processor = HeadroomLimiterAudioProcessor(defaultGainDb = -3.5f)
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT)
        processor.configure(format)
        processor.flush()

        // Set ReplayGain to -6.0 dB
        processor.setTargetGainDb(-6.0f, immediate = true)
        assertEquals(-6.0f, processor.getTargetGainDb(), 0.01f)
        assertEquals(0.501187f, processor.getCurrentLinearGain(), 0.01f)

        // Process a 1.0f sample with -6.0 dB gain (~0.5f linear)
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.putFloat(1.0f)
        input.flip()

        processor.queueInput(input)
        val output = processor.output
        val outSample = output.float
        assertEquals(0.501f, outSample, 0.02f)
    }
}
