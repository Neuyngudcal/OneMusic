package com.example.onemusic.data.scanner.replaygain

import com.example.onemusic.data.scanner.ReplayGainData
import com.example.onemusic.data.scanner.replaygain.FlacVorbisParser.parseVorbisCommentBlock
import java.nio.charset.StandardCharsets

/** OGG Vorbis / Opus: Output Gain trong OpusHead, hoặc Vorbis Comment trong OpusTags / gói vorbis (0x03 "vorbis"). */
internal object OggOpusParser {

    fun parseOggOrOpus(bytes: ByteArray): ReplayGainData {
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
}
