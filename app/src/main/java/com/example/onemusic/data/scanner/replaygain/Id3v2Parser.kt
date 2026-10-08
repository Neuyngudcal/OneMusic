package com.example.onemusic.data.scanner.replaygain

import com.example.onemusic.data.scanner.ReplayGainData
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.decodeSynchsafeInt
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseGainString
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseItunNorm
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseR128Gain
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.readInt32BE
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** MP3 / WAV / AIFF: thẻ ID3v2.2 / 2.3 / 2.4 – TXXX (ReplayGain, R128), RVA2, COMM (iTunNORM), USLT/SYLT (lời bài hát). */
internal object Id3v2Parser {

    fun isId3v2(bytes: ByteArray): Boolean {
        return bytes.size >= 10 &&
                bytes[0] == 0x49.toByte() && // 'I'
                bytes[1] == 0x44.toByte() && // 'D'
                bytes[2] == 0x33.toByte() && // '3'
                (bytes[3].toInt() and 0xFF) < 0xFF &&
                (bytes[4].toInt() and 0xFF) < 0xFF
    }

    fun parseId3(bytes: ByteArray): ReplayGainData {
        if (!isId3v2(bytes)) return ReplayGainData()

        val majorVersion = bytes[3].toInt() and 0xFF
        val flags = bytes[5].toInt() and 0xFF
        val hasExtendedHeader = (flags and 0x40) != 0

        val tagSize = decodeSynchsafeInt(bytes, 6)
        val endOffset = minOf(10 + tagSize, bytes.size)

        var offset = 10
        if (hasExtendedHeader) {
            if (offset + 4 <= endOffset) {
                val extSize = if (majorVersion == 4) decodeSynchsafeInt(bytes, offset) else readInt32BE(bytes, offset)
                offset += (if (majorVersion == 4) extSize else extSize + 4)
            }
        }

        var trackGain: Float? = null
        var trackPeak: Float? = null
        var albumGain: Float? = null
        var albumPeak: Float? = null
        var origin: String? = null
        var embeddedLyrics: String? = null

        val isV22 = majorVersion == 2
        val headerLen = if (isV22) 6 else 10

        while (offset + headerLen <= endOffset) {
            val frameId = if (isV22) {
                String(bytes, offset, 3, StandardCharsets.ISO_8859_1)
            } else {
                String(bytes, offset, 4, StandardCharsets.ISO_8859_1)
            }

            // End of valid frames (padding of 0x00 bytes)
            if (frameId.startsWith("\u0000") || frameId.any { it !in 'A'..'Z' && it !in '0'..'9' }) {
                break
            }

            val frameSize = if (isV22) {
                ((bytes[offset + 3].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 4].toInt() and 0xFF) shl 8) or
                        (bytes[offset + 5].toInt() and 0xFF)
            } else {
                if (majorVersion == 4) decodeSynchsafeInt(bytes, offset + 4)
                else readInt32BE(bytes, offset + 4)
            }

            offset += headerLen
            if (frameSize <= 0 || offset + frameSize > endOffset) break

            val frameBytes = bytes.copyOfRange(offset, offset + frameSize)
            offset += frameSize

            when (frameId) {
                "TXXX", "TXX" -> {
                    val (desc, value) = parseTxxxFrame(frameBytes)
                    val descUpper = desc.uppercase()
                    when {
                        descUpper == "REPLAYGAIN_TRACK_GAIN" -> {
                            parseGainString(value)?.let {
                                trackGain = it
                                if (origin == null) origin = "ReplayGain (ID3 TXXX)"
                            }
                        }
                        descUpper == "REPLAYGAIN_TRACK_PEAK" -> {
                            value.toFloatOrNull()?.let { trackPeak = it }
                        }
                        descUpper == "REPLAYGAIN_ALBUM_GAIN" -> {
                            parseGainString(value)?.let { albumGain = it }
                        }
                        descUpper == "REPLAYGAIN_ALBUM_PEAK" -> {
                            value.toFloatOrNull()?.let { albumPeak = it }
                        }
                        descUpper == "R128_TRACK_GAIN" -> {
                            parseR128Gain(value)?.let {
                                trackGain = it
                                origin = "EBU R128 (ID3 TXXX)"
                            }
                        }
                        descUpper == "R128_ALBUM_GAIN" -> {
                            parseR128Gain(value)?.let { albumGain = it }
                        }
                    }
                }
                "RVA2" -> {
                    // ID3v2.4 Relative Volume Adjustment (accurate 1/512 dB representation)
                    val rvaData = parseRva2Frame(frameBytes)
                    if (rvaData.trackGainDb != null && trackGain == null) {
                        trackGain = rvaData.trackGainDb
                        origin = "ID3 RVA2"
                    }
                    if (rvaData.trackPeak != null && trackPeak == null) {
                        trackPeak = rvaData.trackPeak
                    }
                }
                "COMM", "COM" -> {
                    val (desc, text) = parseCommFrame(frameBytes)
                    if (desc.equals("iTunNORM", ignoreCase = true) || desc.equals("replaygain_track_gain", ignoreCase = true)) {
                        if (desc.equals("iTunNORM", ignoreCase = true)) {
                            parseItunNorm(text)?.let {
                                if (trackGain == null) {
                                    trackGain = it
                                    origin = "iTunNORM SoundCheck"
                                }
                            }
                        } else {
                            parseGainString(text)?.let {
                                if (trackGain == null) {
                                    trackGain = it
                                    origin = "ReplayGain"
                                }
                            }
                        }
                    }
                }
                "USLT", "ULT", "SYLT", "SLT" -> {
                    val lyrics = parseUsltFrame(frameBytes)
                    if (lyrics != null && embeddedLyrics == null) {
                        embeddedLyrics = lyrics
                    }
                }
            }
        }

        return ReplayGainData(
            trackGainDb = trackGain,
            trackPeak = trackPeak,
            albumGainDb = albumGain,
            albumPeak = albumPeak,
            origin = origin,
            embeddedLyrics = embeddedLyrics
        )
    }

    private fun parseUsltFrame(frameBytes: ByteArray): String? {
        if (frameBytes.size < 5) return null
        val encodingByte = frameBytes[0].toInt() and 0xFF
        val charset = id3Charset(encodingByte)
        val nullTermSize = if (encodingByte == 1 || encodingByte == 2) 2 else 1

        // Skip 1 byte encoding + 3 bytes language
        var nullPos = -1
        var i = 4
        while (i <= frameBytes.size - nullTermSize) {
            if (nullTermSize == 1 && frameBytes[i] == 0.toByte()) {
                nullPos = i
                break
            } else if (nullTermSize == 2 && frameBytes[i] == 0.toByte() && frameBytes[i + 1] == 0.toByte()) {
                nullPos = i
                break
            }
            i += nullTermSize
        }

        if (nullPos == -1) return null
        val textStart = nullPos + nullTermSize
        return if (textStart < frameBytes.size) {
            String(frameBytes, textStart, frameBytes.size - textStart, charset).trim().trim('\u0000')
        } else null
    }

    private fun parseTxxxFrame(frameBytes: ByteArray): Pair<String, String> {
        if (frameBytes.isEmpty()) return "" to ""
        val encodingByte = frameBytes[0].toInt() and 0xFF
        val charset = id3Charset(encodingByte)
        val nullTermSize = if (encodingByte == 1 || encodingByte == 2) 2 else 1

        var nullPos = -1
        var i = 1
        while (i <= frameBytes.size - nullTermSize) {
            if (nullTermSize == 1 && frameBytes[i] == 0.toByte()) {
                nullPos = i
                break
            } else if (nullTermSize == 2 && frameBytes[i] == 0.toByte() && frameBytes[i + 1] == 0.toByte()) {
                nullPos = i
                break
            }
            i += nullTermSize
        }

        if (nullPos == -1) {
            val text = String(frameBytes, 1, frameBytes.size - 1, charset).trim().trim('\u0000')
            return text to ""
        }

        val desc = String(frameBytes, 1, nullPos - 1, charset).trim().trim('\u0000')
        val valStart = nullPos + nullTermSize
        val value = if (valStart < frameBytes.size) {
            String(frameBytes, valStart, frameBytes.size - valStart, charset).trim().trim('\u0000')
        } else ""

        return desc to value
    }

    private fun parseCommFrame(frameBytes: ByteArray): Pair<String, String> {
        if (frameBytes.size < 5) return "" to ""
        val encodingByte = frameBytes[0].toInt() and 0xFF
        val charset = id3Charset(encodingByte)
        val nullTermSize = if (encodingByte == 1 || encodingByte == 2) 2 else 1

        // Skip 1 byte encoding + 3 bytes language
        var nullPos = -1
        var i = 4
        while (i <= frameBytes.size - nullTermSize) {
            if (nullTermSize == 1 && frameBytes[i] == 0.toByte()) {
                nullPos = i
                break
            } else if (nullTermSize == 2 && frameBytes[i] == 0.toByte() && frameBytes[i + 1] == 0.toByte()) {
                nullPos = i
                break
            }
            i += nullTermSize
        }

        if (nullPos == -1) return "" to ""
        val desc = String(frameBytes, 4, nullPos - 4, charset).trim().trim('\u0000')
        val textStart = nullPos + nullTermSize
        val text = if (textStart < frameBytes.size) {
            String(frameBytes, textStart, frameBytes.size - textStart, charset).trim().trim('\u0000')
        } else ""
        return desc to text
    }

    private fun parseRva2Frame(frameBytes: ByteArray): ReplayGainData {
        return try {
            // Find null terminator for Identification string
            var nullPos = 0
            while (nullPos < frameBytes.size && frameBytes[nullPos] != 0.toByte()) {
                nullPos++
            }
            var offset = nullPos + 1
            if (offset >= frameBytes.size) return ReplayGainData()

            // Channels loop: 1 byte channel type, 2 bytes volume adj (signed 16-bit / 512.0 dB), 1 byte bits representing peak
            var masterGainDb: Float? = null
            var peakVal: Float? = null

            while (offset + 4 <= frameBytes.size) {
                val channelType = frameBytes[offset].toInt() and 0xFF
                val volAdjRaw = ((frameBytes[offset + 1].toInt()) shl 8) or (frameBytes[offset + 2].toInt() and 0xFF)
                val gainDb = volAdjRaw.toShort().toFloat() / 512.0f
                val peakBits = frameBytes[offset + 3].toInt() and 0xFF
                val peakBytesCount = (peakBits + 7) / 8
                offset += 4 + peakBytesCount

                if (channelType == 1 && masterGainDb == null) { // 1 = Master Volume
                    masterGainDb = gainDb
                } else if (masterGainDb == null) {
                    masterGainDb = gainDb
                }
            }

            ReplayGainData(trackGainDb = masterGainDb, trackPeak = peakVal, origin = "RVA2")
        } catch (_: Exception) {
            ReplayGainData()
        }
    }

    private fun id3Charset(encodingByte: Int): Charset {
        return when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }
    }
}
