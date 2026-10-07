package com.example.onemusic.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class ReplayGainExtractorTest {

    @Test
    fun testParseGainStringFormats() {
        assertEquals(-6.5f, ReplayGainExtractor.parseGainString("-6.50 dB")!!, 0.01f)
        assertEquals(1.2f, ReplayGainExtractor.parseGainString("+1.20 dB")!!, 0.01f)
        assertEquals(-4.32f, ReplayGainExtractor.parseGainString("-4.32 dBTP")!!, 0.01f)
        assertEquals(-8.0f, ReplayGainExtractor.parseGainString("-8.00 LUFS")!!, 0.01f)
        assertEquals(-3.25f, ReplayGainExtractor.parseGainString("-3.25")!!, 0.01f)
    }

    @Test
    fun testParseR128GainQ78() {
        // -1280 in Q7.8 = -1280 / 256 = -5.0 dB
        assertEquals(-5.0f, ReplayGainExtractor.parseR128Gain("-1280")!!, 0.01f)
        // Explicit dB string
        assertEquals(-4.5f, ReplayGainExtractor.parseR128Gain("-4.50 dB")!!, 0.01f)
    }

    @Test
    fun testParseItunNormString() {
        // 1000 in hex is 000003E8 -> -10 * log10(1000/1000) = 0 dB
        val itun0dB = " 000003E8 000003E8 00001000 00001000 00002000 00002000 00003000 00003000"
        assertEquals(0.0f, ReplayGainExtractor.parseItunNorm(itun0dB)!!, 0.01f)

        // 2239 in hex is 000008BF -> -10 * log10(2239/1000) ~= -3.5 dB
        val itunMinus35dB = " 000008BF 000008BF 00001000 00001000 00002000 00002000 00003000 00003000"
        assertEquals(-3.5f, ReplayGainExtractor.parseItunNorm(itunMinus35dB)!!, 0.1f)
    }

    @Test
    fun testFlacVorbisCommentExtraction() {
        // Create synthetic FLAC header with VORBIS_COMMENT block
        val comments = listOf(
            "TITLE=Galaxy Vibe",
            "REPLAYGAIN_TRACK_GAIN=-4.50 dB",
            "REPLAYGAIN_TRACK_PEAK=0.988200",
            "REPLAYGAIN_ALBUM_GAIN=-3.20 dB"
        )

        // Build Vorbis comment payload
        val vendor = "reference libFLAC 1.4.2"
        val vendorBytes = vendor.toByteArray(StandardCharsets.UTF_8)
        
        var totalSize = 4 + vendorBytes.size + 4
        val encodedComments = comments.map { it.toByteArray(StandardCharsets.UTF_8) }
        encodedComments.forEach { totalSize += 4 + it.size }

        val vorbisPayload = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        vorbisPayload.putInt(vendorBytes.size)
        vorbisPayload.put(vendorBytes)
        vorbisPayload.putInt(comments.size)
        encodedComments.forEach {
            vorbisPayload.putInt(it.size)
            vorbisPayload.put(it)
        }
        val vorbisBytes = vorbisPayload.array()

        // FLAC structure: "fLaC" (4 bytes) + Block Header (1 byte type + 3 bytes len) + Block Payload
        val flacBuffer = ByteBuffer.allocate(4 + 4 + vorbisBytes.size)
        flacBuffer.put("fLaC".toByteArray(StandardCharsets.ISO_8859_1))
        
        val isLast = 1
        val blockType = 4 // VORBIS_COMMENT
        val headerByte = ((isLast shl 7) or blockType).toByte()
        flacBuffer.put(headerByte)
        flacBuffer.put(((vorbisBytes.size shr 16) and 0xFF).toByte())
        flacBuffer.put(((vorbisBytes.size shr 8) and 0xFF).toByte())
        flacBuffer.put((vorbisBytes.size and 0xFF).toByte())
        flacBuffer.put(vorbisBytes)

        val stream = ByteArrayInputStream(flacBuffer.array())
        val extracted = ReplayGainExtractor.extract(stream, "flac")

        assertTrue(extracted.hasData)
        assertEquals(-4.50f, extracted.trackGainDb!!, 0.01f)
        assertEquals(0.988200f, extracted.trackPeak!!, 0.0001f)
        assertEquals(-3.20f, extracted.albumGainDb!!, 0.01f)
    }

    @Test
    fun testId3v2TxxxExtraction() {
        // Create synthetic ID3v2.3 tag with TXXX frame for REPLAYGAIN_TRACK_GAIN
        val desc = "REPLAYGAIN_TRACK_GAIN"
        val value = "-5.80 dB"
        val encoding: Byte = 0 // ISO-8859-1
        
        val descBytes = desc.toByteArray(StandardCharsets.ISO_8859_1)
        val valBytes = value.toByteArray(StandardCharsets.ISO_8859_1)
        
        val framePayload = ByteArray(1 + descBytes.size + 1 + valBytes.size)
        framePayload[0] = encoding
        System.arraycopy(descBytes, 0, framePayload, 1, descBytes.size)
        framePayload[1 + descBytes.size] = 0 // Null terminator
        System.arraycopy(valBytes, 0, framePayload, 1 + descBytes.size + 1, valBytes.size)

        // ID3v2.3 Frame Header: FrameID (4) + Size (4) + Flags (2)
        val frameLen = 10 + framePayload.size
        val totalTagSize = frameLen

        val id3Buffer = ByteBuffer.allocate(10 + totalTagSize)
        id3Buffer.put("ID3".toByteArray(StandardCharsets.ISO_8859_1))
        id3Buffer.put(3.toByte()) // v2.3
        id3Buffer.put(0.toByte()) // rev
        id3Buffer.put(0.toByte()) // flags
        
        // 4 bytes synchsafe tag size
        id3Buffer.put(((totalTagSize shr 21) and 0x7F).toByte())
        id3Buffer.put(((totalTagSize shr 14) and 0x7F).toByte())
        id3Buffer.put(((totalTagSize shr 7) and 0x7F).toByte())
        id3Buffer.put((totalTagSize and 0x7F).toByte())

        // Frame
        id3Buffer.put("TXXX".toByteArray(StandardCharsets.ISO_8859_1))
        id3Buffer.putInt(framePayload.size)
        id3Buffer.putShort(0.toShort()) // flags
        id3Buffer.put(framePayload)

        val stream = ByteArrayInputStream(id3Buffer.array())
        val extracted = ReplayGainExtractor.extract(stream, "mp3")

        assertTrue(extracted.hasData)
        assertEquals(-5.80f, extracted.trackGainDb!!, 0.01f)
    }
}
