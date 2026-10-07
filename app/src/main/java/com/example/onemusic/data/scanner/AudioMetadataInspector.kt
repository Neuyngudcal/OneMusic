package com.example.onemusic.data.scanner

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.onemusic.data.model.Track
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AudioTechnicalDetails(
    val format: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val channelCount: Int?,
    val fileSizeBytes: Long,
    val fileSizeFormatted: String,
    val filePath: String,
    val dateModifiedFormatted: String,
    val isHiRes: Boolean,
    val isLossless: Boolean,
    val qualityBadgeText: String
)

object AudioMetadataInspector {

    fun inspectTrack(context: Context, track: Track): AudioTechnicalDetails {
        var format = "Audio"
        var bitrate: Int? = null
        var sampleRate: Int? = null
        var bitDepth: Int? = null
        var channels: Int? = null
        var fileSizeBytes: Long = 0L
        var filePath: String = track.audioUrl
        var lastModified: Long = 0L

        // Try inspecting file directly if audioUrl is a path or content URI
        if (track.audioUrl.isNotBlank()) {
            if (track.audioUrl.startsWith("content://")) {
                try {
                    val uri = Uri.parse(track.audioUrl)
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        fileSizeBytes = pfd.statSize
                    }
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst()) {
                            if (sizeIdx != -1 && fileSizeBytes <= 0) {
                                fileSizeBytes = cursor.getLong(sizeIdx)
                            }
                            if (nameIdx != -1) {
                                val name = cursor.getString(nameIdx)
                                if (!name.isNullOrBlank()) {
                                    val ext = name.substringAfterLast('.', "").uppercase(Locale.getDefault())
                                    if (ext.isNotBlank()) format = ext
                                    filePath = name
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            } else {
                val file = File(track.audioUrl)
                if (file.exists()) {
                    fileSizeBytes = file.length()
                    lastModified = file.lastModified()
                    val ext = file.extension.uppercase(Locale.getDefault())
                    if (ext.isNotBlank()) format = ext
                }
            }
        }

        // Use MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            if (track.audioUrl.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(track.audioUrl))
            } else if (track.audioUrl.isNotBlank()) {
                val file = File(track.audioUrl)
                if (file.exists()) {
                    retriever.setDataSource(track.audioUrl)
                }
            }

            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!brStr.isNullOrBlank()) {
                bitrate = brStr.toIntOrNull()?.let { it / 1000 }
            }

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mime.isNullOrBlank()) {
                format = when {
                    mime.contains("flac", ignoreCase = true) -> "FLAC"
                    mime.contains("mp3", ignoreCase = true) || mime.contains("mpeg", ignoreCase = true) -> "MP3"
                    mime.contains("aac", ignoreCase = true) || mime.contains("mp4", ignoreCase = true) -> "AAC"
                    mime.contains("wav", ignoreCase = true) || mime.contains("wave", ignoreCase = true) -> "WAV"
                    mime.contains("ogg", ignoreCase = true) || mime.contains("vorbis", ignoreCase = true) -> "OGG"
                    mime.contains("opus", ignoreCase = true) -> "OPUS"
                    mime.contains("alac", ignoreCase = true) -> "ALAC"
                    else -> format
                }
            }
        } catch (_: Throwable) {
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }

        // Use MediaExtractor for deep audio track stream info (sample rate, channels, bit depth)
        val extractor = MediaExtractor()
        try {
            if (track.audioUrl.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(track.audioUrl), null)
            } else if (track.audioUrl.isNotBlank()) {
                val file = File(track.audioUrl)
                if (file.exists()) {
                    extractor.setDataSource(track.audioUrl)
                }
            }

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (trackFormat.containsKey(MediaFormat.KEY_BIT_RATE) && bitrate == null) {
                        bitrate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    if (trackFormat.containsKey("pcm-encoding") || trackFormat.containsKey("bit-width")) {
                        try {
                            bitDepth = if (trackFormat.containsKey("bit-width")) trackFormat.getInteger("bit-width") else 16
                        } catch (_: Throwable) {}
                    }
                    break
                }
            }
        } catch (_: Throwable) {
        } finally {
            try { extractor.release() } catch (_: Throwable) {}
        }

        if (sampleRate == null) sampleRate = 44100
        if (channels == null) channels = 2
        val effectiveBitDepth = bitDepth ?: if (format == "FLAC" || format == "WAV") 24 else 16

        val isHiRes = (sampleRate >= 96000 || effectiveBitDepth >= 24)
        val isLossless = format == "FLAC" || format == "WAV" || format == "ALAC" || isHiRes
        val qualityBadge = when {
            isHiRes -> "Hi-Res Audio (24-bit / ${sampleRate / 1000}kHz)"
            isLossless -> "Lossless ($format)"
            (bitrate ?: 0) >= 320 -> "High Quality (320 kbps)"
            else -> "$format Standard"
        }

        val sizeFormatted = formatFileSize(fileSizeBytes)
        val dateFormatted = if (lastModified > 0) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))
        } else {
            "Không rõ"
        }

        return AudioTechnicalDetails(
            format = format,
            bitrateKbps = bitrate,
            sampleRateHz = sampleRate,
            bitDepth = effectiveBitDepth,
            channelCount = channels,
            fileSizeBytes = fileSizeBytes,
            fileSizeFormatted = sizeFormatted,
            filePath = filePath,
            dateModifiedFormatted = dateFormatted,
            isHiRes = isHiRes,
            isLossless = isLossless,
            qualityBadgeText = qualityBadge
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            else -> String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}
