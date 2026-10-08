package com.example.onemusic.data.scanner.replaygain

import kotlin.math.log10

/** Đổi chuỗi trong thẻ thành dB (ReplayGain, EBU R128, iTunNORM) và đọc số nguyên trong header ID3. */
internal object GainValueParsers {

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

    fun decodeSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
                ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
                ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
                (bytes[offset + 3].toInt() and 0x7F)
    }

    fun readInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }
}
