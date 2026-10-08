package com.example.onemusic.data.scanner.replaygain

import com.example.onemusic.data.scanner.ReplayGainData
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseGainString
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseR128Gain
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/** FLAC: tìm khối METADATA_BLOCK_VORBIS_COMMENT; Vorbis Comment (dùng chung cho FLAC / OGG / Opus): REPLAYGAIN_*, R128_*, LYRICS. */
internal object FlacVorbisParser {

    fun isFlac(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x66.toByte() && // 'f'
                bytes[1] == 0x4C.toByte() && // 'L'
                bytes[2] == 0x61.toByte() && // 'a'
                bytes[3] == 0x43.toByte()    // 'C'
    }

    fun parseFlac(bytes: ByteArray): ReplayGainData {
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
}
