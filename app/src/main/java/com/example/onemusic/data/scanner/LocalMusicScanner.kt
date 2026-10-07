package com.example.onemusic.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.onemusic.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LocalMusicScanner {

    const val MIN_AUDIO_DURATION_MS = 30_000L // 30 seconds
    const val MIN_AUDIO_SIZE_BYTES = 100_000L // 100 KB

    private val EXCLUDED_DIR_NAMES = setOf(
        "whatsapp voice notes", "voice notes", "voice recorder", "recordings",
        "sound recorder", "ringtones", "notifications", "alarms", "call recordings", "callrecordings"
    )

    private val SUPPORTED_EXTENSIONS = setOf(
        "flac", "wav", "mp3", "m4a", "aac", "ogg", "dsf", "dff", "alac", "aiff", "opus", "wma"
    )

    /**
     * Unified music scan: Queries user-configured custom folders via Storage Access Framework (SAF).
     * Only scans folders that are currently attached and accessible.
     */
    suspend fun scanAll(
        context: Context,
        customFolderUris: List<String>,
        cachedTracks: List<Track> = emptyList()
    ): List<Track> = withContext(Dispatchers.IO) {
        val cachedMap = cachedTracks.associateBy { it.audioUrl }
        val allScanned = mutableListOf<Track>()

        // Scan only user-configured custom folders via SAF
        for (folderUriStr in customFolderUris) {
            try {
                val uri = Uri.parse(folderUriStr)
                if (isFolderAccessible(context, uri)) {
                    val folderTracks = scanFolder(context, uri, cachedMap)
                    allScanned.addAll(folderTracks)
                }
            } catch (_: Exception) {}
        }

        // Deduplicate tracks by audio URL and normalized title+artist
        allScanned.distinctBy { it.audioUrl }
            .distinctBy { "${it.title.trim().lowercase()}_${it.artist.trim().lowercase()}" }
    }

    /**
     * Scans Android MediaStore for all audio tracks marked as music (duration >= 30s and size >= 100KB).
     */
    suspend fun scanMediaStore(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val scanned = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE
        )

        val shouldFilterShort = try {
            com.example.onemusic.data.local.SettingsPreferences(context).getSettings().filterShortAudio
        } catch (_: Exception) { true }
        val minDuration = if (shouldFilterShort) MIN_AUDIO_DURATION_MS else 5_000L

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDuration AND ${MediaStore.Audio.Media.SIZE} >= $MIN_AUDIO_SIZE_BYTES"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val mmr = MediaMetadataRetriever()

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol).orEmpty()
                    val rawArtist = c.getString(artistCol).orEmpty()
                    val rawAlbum = c.getString(albumCol).orEmpty()
                    val durationMs = c.getLong(durCol)
                    val filePath = c.getString(dataCol).orEmpty()
                    val albumId = c.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )

                    val track = parseMediaStoreTrack(
                        context = context,
                        mediaId = mediaId,
                        rawTitle = rawTitle,
                        rawArtist = rawArtist,
                        rawAlbum = rawAlbum,
                        durationMs = durationMs,
                        filePath = filePath,
                        albumId = albumId,
                        contentUri = contentUri,
                        mmr = mmr,
                        minDurationMs = minDuration
                    )

                    if (track != null) {
                        scanned.add(track)
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or querying error handled gracefully
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {}
        }

        scanned
    }

    private fun parseMediaStoreTrack(
        context: Context,
        mediaId: Long,
        rawTitle: String,
        rawArtist: String,
        rawAlbum: String,
        durationMs: Long,
        filePath: String,
        albumId: Long,
        contentUri: Uri,
        mmr: MediaMetadataRetriever,
        minDurationMs: Long = MIN_AUDIO_DURATION_MS
    ): Track? {
        // Strict Audio Filter: Discard audio tracks shorter than minDurationMs
        if (durationMs in 1 until minDurationMs) {
            return null
        }

        val ext = filePath.substringAfterLast('.', "").lowercase().ifEmpty { "mp3" }
        val id = "ms_${mediaId}"
        val title = rawTitle.ifBlank { filePath.substringAfterLast('/').substringBeforeLast('.') }.ifBlank { "Bài hát chưa đặt tên" }
        val artist = if (rawArtist.isBlank() || rawArtist == "<unknown>") "Nghệ sĩ chưa rõ" else rawArtist
        val album = if (rawAlbum.isBlank() || rawAlbum == "<unknown>") "Thư viện thiết bị" else rawAlbum

        var isHiRes = ext in setOf("flac", "wav", "dsf", "dff", "aiff", "alac")
        var bitRateLabel = when {
            ext == "flac" -> "24-bit / 96kHz FLAC"
            ext in setOf("dsf", "dff") -> "DSD 5.6MHz Hi-Res"
            ext == "wav" -> "24-bit / 192kHz WAV"
            ext == "alac" || ext == "aiff" -> "Lossless ${ext.uppercase()}"
            else -> "${ext.uppercase()} Audio"
        }

        var artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=800&auto=format&fit=crop"

        try {
            mmr.setDataSource(context, contentUri)
            val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            if (bitrate != null && bitrate > 0) {
                if (bitrate >= 500_000) isHiRes = true
                bitRateLabel = "${bitrate / 1000} kbps ${ext.uppercase()}"
            }

            val pic = mmr.embeddedPicture
            if (pic != null && pic.isNotEmpty()) {
                artworkUrl = saveArtworkToCache(context, id, pic)
            } else if (albumId > 0) {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                artworkUrl = albumArtUri.toString()
            }
        } catch (_: Exception) {
            if (albumId > 0) {
                artworkUrl = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()
            }
        }

        val replayGainData = try {
            context.contentResolver.openInputStream(contentUri)?.use { stream ->
                ReplayGainExtractor.extract(stream, ext)
            } ?: ReplayGainData()
        } catch (_: Exception) {
            ReplayGainData()
        }

        val lyrics = if (!replayGainData.embeddedLyrics.isNullOrBlank()) {
            LrcParser.parseLrc(replayGainData.embeddedLyrics, durationMs)
        } else {
            if (filePath.isNotBlank()) {
                try {
                    val lrcFile = File(filePath.substringBeforeLast('.') + ".lrc")
                    if (lrcFile.exists() && lrcFile.canRead()) {
                        LrcParser.parseLrc(lrcFile.readText(), durationMs)
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()
        }

        val directFile = if (filePath.isNotBlank()) File(filePath) else null
        val audioUrl = if (directFile != null && try { directFile.exists() && directFile.canRead() } catch (_: Exception) { false }) {
            Uri.fromFile(directFile).toString()
        } else {
            contentUri.toString()
        }

        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = if (durationMs > 0) durationMs else 180000L,
            audioUrl = audioUrl,
            artworkUrl = artworkUrl,
            isHiRes = isHiRes,
            isDolbyAtmos = isHiRes,
            bitRate = bitRateLabel,
            lyrics = lyrics,
            isFavorite = false,
            replayGainDb = replayGainData.effectiveGainDb,
            peakLevel = replayGainData.trackPeak ?: replayGainData.albumPeak,
            replayGainOrigin = replayGainData.origin
        )
    }

    suspend fun scanFolder(
        context: Context,
        folderUri: Uri,
        cachedTracksMap: Map<String, Track> = emptyMap()
    ): List<Track> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val scannedTracks = mutableListOf<Track>()
        val queue = ArrayDeque<DocumentFile>()
        queue.add(rootDoc)

        val mmr = MediaMetadataRetriever()

        try {
            while (queue.isNotEmpty()) {
                val currentDir = queue.removeFirst()
                val dirName = currentDir.name?.lowercase().orEmpty()
                if (dirName.startsWith(".") || (currentDir != rootDoc && dirName in EXCLUDED_DIR_NAMES)) {
                    continue
                }

                val files = try {
                    currentDir.listFiles()
                } catch (_: Exception) {
                    emptyArray()
                }

                // Skip directory if it contains a .nomedia file
                if (files.any { it.isFile && it.name?.equals(".nomedia", ignoreCase = true) == true }) {
                    continue
                }

                // Index all .lrc files in the directory by their base filename (lowercased)
                val lrcFilesMap = files.filter { it.isFile && (it.name?.endsWith(".lrc", ignoreCase = true) == true) }
                    .associateBy { it.name?.substringBeforeLast('.')?.lowercase().orEmpty() }

                for (file in files) {
                    if (file.isDirectory) {
                        val subDirName = file.name?.lowercase().orEmpty()
                        if (!subDirName.startsWith(".") && subDirName !in EXCLUDED_DIR_NAMES) {
                            queue.add(file)
                        }
                    } else if (file.isFile) {
                        val fileName = file.name ?: continue
                        if (fileName.startsWith(".")) continue

                        val ext = fileName.substringAfterLast('.', "").lowercase()
                        if (ext in SUPPORTED_EXTENSIONS) {
                            val audioUrl = file.uri.toString()
                            val cachedTrack = cachedTracksMap[audioUrl]
                            if (cachedTrack != null) {
                                scannedTracks.add(cachedTrack)
                            } else {
                                val baseName = fileName.substringBeforeLast('.').lowercase()
                                val matchingLrcFile = lrcFilesMap[baseName]
                                val track = extractTrackMetadata(context, file, ext, mmr, matchingLrcFile)
                                if (track != null) {
                                    scannedTracks.add(track)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {}
        }

        scannedTracks
    }

    private fun extractTrackMetadata(
        context: Context,
        docFile: DocumentFile,
        ext: String,
        mmr: MediaMetadataRetriever,
        matchingLrcFile: DocumentFile? = null
    ): Track? {
        // 1. Filter out tiny files (< 100KB: notification sounds, UI sound clips, tiny corrupted records)
        val fileSize = try { docFile.length() } catch (_: Exception) { 0L }
        if (fileSize in 1 until MIN_AUDIO_SIZE_BYTES) {
            return null
        }

        val fileUri = docFile.uri
        val fileName = docFile.name ?: "Unknown"
        val id = "local_" + UUID.nameUUIDFromBytes(fileUri.toString().toByteArray()).toString().take(12)

        return try {
            mmr.setDataSource(context, fileUri)

            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            val shouldFilterShort = try {
                com.example.onemusic.data.local.SettingsPreferences(context).getSettings().filterShortAudio
            } catch (_: Exception) { true }
            val minDuration = if (shouldFilterShort) MIN_AUDIO_DURATION_MS else 5_000L

            // 2. Audio Filter: Discard audio tracks shorter than minDuration (30s if filterShortAudio is true, 5s otherwise)
            if (durationMs in 1 until minDuration) {
                return null
            }

            val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                ?: fileName.substringBeforeLast('.')
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() }
                ?: "Nghệ sĩ chưa rõ"
            val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
                ?: "Thư mục cục bộ"
            val bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()

            val isHiRes = ext in setOf("flac", "wav", "dsf", "dff", "aiff", "alac") ||
                    (bitrate != null && bitrate >= 500_000)

            val bitRateLabel = when {
                ext == "flac" -> "24-bit / 96kHz FLAC"
                ext in setOf("dsf", "dff") -> "DSD 5.6MHz Hi-Res"
                ext == "wav" -> "24-bit / 192kHz WAV"
                ext == "alac" || ext == "aiff" -> "Lossless ${ext.uppercase()}"
                bitrate != null && bitrate > 0 -> "${bitrate / 1000} kbps ${ext.uppercase()}"
                else -> "${ext.uppercase()} Audio"
            }

            val embeddedPic = mmr.embeddedPicture
            val artworkUrl = if (embeddedPic != null && embeddedPic.isNotEmpty()) {
                saveArtworkToCache(context, id, embeddedPic)
            } else {
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=800&auto=format&fit=crop"
            }

            // Extract ReplayGain (EBU R128 / ReplayGain 2.0 / iTunNORM) and embedded lyrics from audio stream
            val replayGainData = try {
                context.contentResolver.openInputStream(fileUri)?.use { stream ->
                    ReplayGainExtractor.extract(stream, ext)
                } ?: ReplayGainData()
            } catch (_: Exception) {
                ReplayGainData()
            }

            // 1. Check matching local .lrc file in the same folder
            var lyrics = if (matchingLrcFile != null) {
                try {
                    context.contentResolver.openInputStream(matchingLrcFile.uri)?.use { stream ->
                        val lrcText = stream.bufferedReader().readText()
                        LrcParser.parseLrc(lrcText, durationMs)
                    } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            // 2. If no local .lrc file, check embedded lyrics in audio tags (ID3 USLT, Vorbis LYRICS, MP4 ©lyr)
            if (lyrics.isEmpty() && !replayGainData.embeddedLyrics.isNullOrBlank()) {
                lyrics = LrcParser.parseLrc(replayGainData.embeddedLyrics, durationMs)
            }

            Track(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = if (durationMs > 0) durationMs else 180000L,
                audioUrl = fileUri.toString(),
                artworkUrl = artworkUrl,
                isHiRes = isHiRes,
                isDolbyAtmos = isHiRes,
                bitRate = bitRateLabel,
                lyrics = lyrics,
                isFavorite = false,
                replayGainDb = replayGainData.effectiveGainDb,
                peakLevel = replayGainData.trackPeak ?: replayGainData.albumPeak,
                replayGainOrigin = replayGainData.origin
            )
        } catch (_: Exception) {
            // Fallback track creation when metadata retriever fails on certain formats
            Track(
                id = id,
                title = fileName.substringBeforeLast('.'),
                artist = "Nghệ sĩ chưa rõ",
                album = "Thư mục cục bộ",
                durationMs = 180000L,
                audioUrl = fileUri.toString(),
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=800&auto=format&fit=crop",
                isHiRes = ext in setOf("flac", "wav", "dsf", "dff"),
                bitRate = "${ext.uppercase()} Audio",
                lyrics = emptyList(),
                isFavorite = false,
                replayGainDb = null,
                peakLevel = null,
                replayGainOrigin = null
            )
        }
    }

    private fun saveArtworkToCache(context: Context, trackId: String, pictureBytes: ByteArray): String {
        return try {
            val artDir = File(context.cacheDir, "artworks").apply { if (!exists()) mkdirs() }
            val artFile = File(artDir, "${trackId}.jpg")
            if (!artFile.exists() || artFile.length() == 0L) {
                FileOutputStream(artFile).use { it.write(pictureBytes) }
            }
            artFile.toURI().toString()
        } catch (_: Exception) {
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=800&auto=format&fit=crop"
        }
    }

    fun isFolderAccessible(context: Context, folderUri: Uri): Boolean {
        return try {
            val doc = DocumentFile.fromTreeUri(context, folderUri)
            doc != null && doc.exists() && doc.canRead()
        } catch (_: Exception) {
            false
        }
    }

    fun isTrackAccessible(context: Context, track: Track): Boolean {
        return try {
            val uri = Uri.parse(track.audioUrl)
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return false
                    val file = File(path)
                    file.exists() && file.canRead()
                }
                "content" -> {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun cleanOrphanedArtworks(context: Context, activeTrackIds: Set<String>) {
        try {
            val artDir = File(context.cacheDir, "artworks")
            if (artDir.exists() && artDir.isDirectory) {
                artDir.listFiles()?.forEach { file ->
                    val id = file.nameWithoutExtension
                    if (id !in activeTrackIds) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
