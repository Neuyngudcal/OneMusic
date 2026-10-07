package com.example.onemusic.data.scanner

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.log10

/**
 * Extracted ReplayGain and EBU R128 loudness metadata.
 */
data class ReplayGainData(
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null,
    val origin: String? = null,
    val embeddedLyrics: String? = null
) {
    val effectiveGainDb: Float?
        get() = trackGainDb ?: albumGainDb

    val hasData: Boolean
        get() = trackGainDb != null || albumGainDb != null || embeddedLyrics != null
}

/**
 * Audiophile-grade, lightweight binary metadata extractor for ReplayGain (1.0 & 2.0)
 * and ITU-R BS.1770 / EBU R128 loudness tags across major audio formats:
 * - FLAC (Vorbis Comments Metadata Block)
 * - MP3 / WAV / AIFF (ID3v2.2, ID3v2.3, ID3v2.4 - TXXX, RVA2, RGAD, COMM)
 * - M4A / MP4 / AAC (QuickTime ilst tags: '----' custom tags, iTunNORM SoundCheck)
 * - OGG Vorbis / Opus (Vorbis Comments & OpusHead Output Gain)
 */
object ReplayGainExtractor {

    private const val MAX_HEADER_SCAN_BYTES = 512 * 1024 // 512 KB scan buffer limit

    fun extract(inputStream: InputStream, extension: String): ReplayGainData {
        return try {
            val headerBytes = readHeaderBytes(inputStream, MAX_HEADER_SCAN_BYTES)
            if (headerBytes.isEmpty()) return ReplayGainData()

            val ext = extension.lowercase()
            when {
                ext == "flac" || isFlac(headerBytes) -> parseFlac(headerBytes)
                ext in setOf("mp3", "wav", "aiff", "aif") || isId3v2(headerBytes) -> parseId3(headerBytes)
                ext in setOf("m4a", "mp4", "aac", "alac") || isMp4(headerBytes) -> parseMp4(headerBytes)
                ext in setOf("ogg", "opus") -> parseOggOrOpus(headerBytes)
                else -> {
                    // Try ID3 first, then FLAC, then MP4 fallback
                    if (isId3v2(headerBytes)) parseId3(headerBytes)
                    else if (isFlac(headerBytes)) parseFlac(headerBytes)
                    else if (isMp4(headerBytes)) parseMp4(headerBytes)
                    else ReplayGainData()
                }
            }
        } catch (_: Exception) {
            ReplayGainData()
        }
    }

    private fun readHeaderBytes(input: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes)
        var totalRead = 0
        while (totalRead < maxBytes) {
            val read = input.read(buffer, totalRead, maxBytes - totalRead)
            if (read <= 0) break
            totalRead += read
        }
        return if (totalRead == maxBytes) buffer else buffer.copyOf(totalRead)
    }

    // =========================================================================
    // 1. FLAC Vorbis Comment Parser
    // =========================================================================

    private fun isFlac(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x66.toByte() && // 'f'
                bytes[1] == 0x4C.toByte() && // 'L'
                bytes[2] == 0x61.toByte() && // 'a'
                bytes[3] == 0x43.toByte()    // 'C'
    }

    private fun parseFlac(bytes: ByteArray): ReplayGainData {
        if (!isFlac(bytes)) return ReplayGainData()

        var offset = 4
        while (offset + 4 <= bytes.size) {
            val headerByte = bytes[offset].toInt() and 0xFF
            val isLast = (headerByte and 0x80) != 0
            val blockType = headerByte and 0x7F
            val blockLength = ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 3].toInt() and 0xFF)

            offset += 4
            if (offset + blockLength > bytes.size) {
                // Truncated block inside scan buffer, try parsing what we have if it's VORBIS_COMMENT
                if (blockType == 4) {
                    val remaining = bytes.size - offset
                    return parseVorbisCommentBlock(bytes, offset, remaining, "FLAC Vorbis")
                }
                break
            }

            if (blockType == 4) { // METADATA_BLOCK_VORBIS_COMMENT
                return parseVorbisCommentBlock(bytes, offset, blockLength, "FLAC Vorbis")
            }

            if (isLast) break
            offset += blockLength
        }
        return ReplayGainData()
    }

    // =========================================================================
    // 2. Vorbis Comment (FLAC / OGG / OPUS) List Decoder
    // =========================================================================

    fun parseVorbisCommentBlock(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        sourceType: String
    ): ReplayGainData {
        return try {
            val buffer = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN)

            // Vendor string
            if (buffer.remaining() < 4) return ReplayGainData()
            val vendorLen = buffer.int
            if (vendorLen < 0 || vendorLen > buffer.remaining()) return ReplayGainData()
            buffer.position(buffer.position() + vendorLen)

            // User comments count
            if (buffer.remaining() < 4) return ReplayGainData()
            val commentCount = buffer.int
            if (commentCount < 0) return ReplayGainData()

            var trackGain: Float? = null
            var trackPeak: Float? = null
            var albumGain: Float? = null
            var albumPeak: Float? = null
            var origin = sourceType
            var embeddedLyrics: String? = null

            for (i in 0 until minOf(commentCount, 256)) {
                if (buffer.remaining() < 4) break
                val commentLen = buffer.int
                if (commentLen < 0 || commentLen > buffer.remaining()) break

                val commentBytes = ByteArray(commentLen)
                buffer.get(commentBytes)
                val commentStr = String(commentBytes, StandardCharsets.UTF_8)

                val eqIdx = commentStr.indexOf('=')
                if (eqIdx > 0) {
                    val key = commentStr.substring(0, eqIdx).trim().uppercase()
                    val value = commentStr.substring(eqIdx + 1).trim()

                    when (key) {
                        "REPLAYGAIN_TRACK_GAIN" -> {
                            parseGainString(value)?.let {
                                trackGain = it
                                origin = "ReplayGain"
                            }
                        }
                        "REPLAYGAIN_TRACK_PEAK" -> {
                            value.toFloatOrNull()?.let { trackPeak = it }
                        }
                        "REPLAYGAIN_ALBUM_GAIN" -> {
                            parseGainString(value)?.let { albumGain = it }
                        }
                        "REPLAYGAIN_ALBUM_PEAK" -> {
                            value.toFloatOrNull()?.let { albumPeak = it }
                        }
                        "R128_TRACK_GAIN" -> {
                            parseR128Gain(value)?.let {
                                trackGain = it
                                origin = "EBU R128"
                            }
                        }
                        "R128_ALBUM_GAIN" -> {
                            parseR128Gain(value)?.let { albumGain = it }
                        }
                        "LYRICS", "UNSYNCEDLYRICS", "SYNCEDLYRICS" -> {
                            if (embeddedLyrics == null && value.isNotBlank()) {
                                embeddedLyrics = value
                            }
                        }
                    }
                }
            }

            ReplayGainData(
                trackGainDb = trackGain,
                trackPeak = trackPeak,
                albumGainDb = albumGain,
                albumPeak = albumPeak,
                origin = if (trackGain != null || albumGain != null) origin else null,
                embeddedLyrics = embeddedLyrics
            )
        } catch (_: Exception) {
            ReplayGainData()
        }
    }

    // =========================================================================
    // 3. ID3v2 Parser (ID3v2.2, ID3v2.3, ID3v2.4)
    // =========================================================================

    private fun isId3v2(bytes: ByteArray): Boolean {
        return bytes.size >= 10 &&
                bytes[0] == 0x49.toByte() && // 'I'
                bytes[1] == 0x44.toByte() && // 'D'
                bytes[2] == 0x33.toByte() && // '3'
                (bytes[3].toInt() and 0xFF) < 0xFF &&
                (bytes[4].toInt() and 0xFF) < 0xFF
    }

    private fun parseId3(bytes: ByteArray): ReplayGainData {
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

    // =========================================================================
    // 4. MP4 / M4A Atom Parser ('ilst', '----', 'iTunNORM')
    // =========================================================================

    private fun isMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val brand = String(bytes, 4, 4, StandardCharsets.ISO_8859_1)
        return brand == "ftyp" || brand == "moov" || brand == "M4A "
    }

    private fun parseMp4(bytes: ByteArray): ReplayGainData {
        var trackGain: Float? = null
        var trackPeak: Float? = null
        var origin: String? = null

        // Search for 'iTunNORM' or 'replaygain_track_gain' string in metadata area
        val content = String(bytes, StandardCharsets.ISO_8859_1)

        val itunIdx = content.indexOf("iTunNORM")
        if (itunIdx != -1) {
            val chunk = content.substring(itunIdx, minOf(itunIdx + 200, content.length))
            parseItunNorm(chunk)?.let {
                trackGain = it
                origin = "iTunNORM SoundCheck"
            }
        }

        val rgIdx = content.indexOf("replaygain_track_gain", ignoreCase = true)
        if (rgIdx != -1) {
            val chunk = content.substring(rgIdx, minOf(rgIdx + 100, content.length))
            parseGainString(chunk)?.let {
                trackGain = it
                origin = "ReplayGain (MP4)"
            }
        }

        val peakIdx = content.indexOf("replaygain_track_peak", ignoreCase = true)
        if (peakIdx != -1) {
            val chunk = content.substring(peakIdx, minOf(peakIdx + 60, content.length))
            val match = Regex("""([0-9]+\.[0-9]+)""").find(chunk)
            match?.groupValues?.get(1)?.toFloatOrNull()?.let { trackPeak = it }
        }

        return ReplayGainData(trackGainDb = trackGain, trackPeak = trackPeak, origin = origin)
    }

    // =========================================================================
    // 5. OGG Vorbis / Opus Stream Parser
    // =========================================================================

    private fun parseOggOrOpus(bytes: ByteArray): ReplayGainData {
        val content = String(bytes, StandardCharsets.ISO_8859_1)

        // Check OpusHead Output Gain
        val opusHeadIdx = content.indexOf("OpusHead")
        if (opusHeadIdx != -1 && opusHeadIdx + 18 <= bytes.size) {
            val outputGainRaw = ((bytes[opusHeadIdx + 17].toInt()) shl 8) or (bytes[opusHeadIdx + 16].toInt() and 0xFF)
            val opusGainDb = outputGainRaw.toShort().toFloat() / 256.0f
            if (opusGainDb != 0.0f) {
                return ReplayGainData(trackGainDb = opusGainDb, origin = "OpusHead Gain")
            }
        }

        // Check OpusTags or Vorbis Comment block in Ogg stream
        val opusTagsIdx = content.indexOf("OpusTags")
        if (opusTagsIdx != -1) {
            val offset = opusTagsIdx + 8
            if (offset < bytes.size) {
                return parseVorbisCommentBlock(bytes, offset, bytes.size - offset, "Opus EBU R128")
            }
        }

        val vorbisIdx = content.indexOf("\u0003vorbis")
        if (vorbisIdx != -1) {
            val offset = vorbisIdx + 7
            if (offset < bytes.size) {
                return parseVorbisCommentBlock(bytes, offset, bytes.size - offset, "OGG Vorbis")
            }
        }

        return ReplayGainData()
    }

    // =========================================================================
    // 6. Utility & Value Converters
    // =========================================================================

    fun parseGainString(input: String): Float? {
        val match = Regex("""([+-]?[0-9]+(?:\.[0-9]+)?)""").find(input) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        return value.coerceIn(-30.0f, 15.0f)
    }

    fun parseR128Gain(input: String): Float? {
        val match = Regex("""([+-]?[0-9]+(?:\.[0-9]+)?)""").find(input) ?: return null
        val clean = match.groupValues[1]
        val floatVal = clean.toFloatOrNull() ?: return null
        if (!clean.contains('.') && Math.abs(floatVal) >= 50f) {
            return (floatVal / 256.0f).coerceIn(-30.0f, 15.0f)
        }
        return floatVal.coerceIn(-30.0f, 15.0f)
    }

    /**
     * Parses Apple iTunes SoundCheck iTunNORM string.
     * Example: " 00000450 00000450 00001B20 00001B20 00021C75 00021C75 00007E5C 00007E5C 00024B10 00024B10"
     * Gain (dB) = -10 * log10(val / 1000.0)
     */
    fun parseItunNorm(input: String): Float? {
        return try {
            val tokens = input.trim().split(Regex("""\s+""")).filter { it.length == 8 }
            if (tokens.isNotEmpty()) {
                val rawVal = tokens[0].toIntOrNull(16) ?: return null
                if (rawVal > 0) {
                    val gainDb = (-10.0 * log10(rawVal.toDouble() / 1000.0)).toFloat()
                    return gainDb.coerceIn(-30.0f, 15.0f)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
                ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
                ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
                (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun readInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }
}
