package com.example.onemusic.data.scanner

import com.example.onemusic.data.scanner.replaygain.FlacVorbisParser
import com.example.onemusic.data.scanner.replaygain.FlacVorbisParser.isFlac
import com.example.onemusic.data.scanner.replaygain.FlacVorbisParser.parseFlac
import com.example.onemusic.data.scanner.replaygain.GainValueParsers
import com.example.onemusic.data.scanner.replaygain.Id3v2Parser.isId3v2
import com.example.onemusic.data.scanner.replaygain.Id3v2Parser.parseId3
import com.example.onemusic.data.scanner.replaygain.Mp4AtomParser.isMp4
import com.example.onemusic.data.scanner.replaygain.Mp4AtomParser.parseMp4
import com.example.onemusic.data.scanner.replaygain.OggOpusParser.parseOggOrOpus
import java.io.InputStream

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
 *
 * Mặt tiền: nhận dạng định dạng rồi gọi parser tương ứng trong package `replaygain`.
 * Các hàm public cũ (parseGainString, parseR128Gain, parseItunNorm, parseVorbisCommentBlock) giữ nguyên chữ ký.
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

    fun parseVorbisCommentBlock(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        sourceType: String
    ): ReplayGainData = FlacVorbisParser.parseVorbisCommentBlock(bytes, offset, length, sourceType)

    fun parseGainString(input: String): Float? = GainValueParsers.parseGainString(input)

    fun parseR128Gain(input: String): Float? = GainValueParsers.parseR128Gain(input)

    /**
     * Parses Apple iTunes SoundCheck iTunNORM string.
     * Example: " 00000450 00000450 00001B20 00001B20 00021C75 00021C75 00007E5C 00007E5C 00024B10 00024B10"
     * Gain (dB) = -10 * log10(val / 1000.0)
     */
    fun parseItunNorm(input: String): Float? = GainValueParsers.parseItunNorm(input)
}
