package com.example.onemusic.data.scanner.replaygain

import com.example.onemusic.data.scanner.ReplayGainData
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseGainString
import com.example.onemusic.data.scanner.replaygain.GainValueParsers.parseItunNorm
import java.nio.charset.StandardCharsets

/** M4A / MP4 / AAC: tìm chuỗi 'iTunNORM' và 'replaygain_track_gain/peak' trong vùng metadata. */
internal object Mp4AtomParser {

    fun isMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val brand = String(bytes, 4, 4, StandardCharsets.ISO_8859_1)
        return brand == "ftyp" || brand == "moov" || brand == "M4A "
    }

    fun parseMp4(bytes: ByteArray): ReplayGainData {
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
}
