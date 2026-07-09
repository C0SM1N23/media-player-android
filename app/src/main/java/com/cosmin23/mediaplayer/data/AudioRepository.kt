package com.cosmin23.mediaplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile

/**
 * Single source of truth for loading the on-device music library.
 *
 * Consolidates the two loaders that previously existed (`AudioRepository` and the free
 * `loadAudioFromMediaStore` function) and enriches every track with artist, album, artwork,
 * track number, year, size and path so the rest of the app can show rich metadata.
 *
 * All methods are blocking and must be called off the main thread (see [PlayerViewModel]).
 */
class AudioRepository(private val context: Context) {

    private val albumArtBase: Uri = Uri.parse("content://media/external/audio/albumart")

    /** Loads all music tracks from MediaStore with full metadata. */
    fun loadFromMediaStore(): List<AudioItem> {
        // RELATIVE_PATH only exists on API 29+; querying it on older devices throws, so add it
        // to the projection conditionally (minSdk is 26).
        val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (hasRelativePath) add(MediaStore.Audio.Media.RELATIVE_PATH)
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = ArrayList<AudioItem>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            // -1 when the column is absent (API < 29); getColumnIndex never throws.
            val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val artist = cursor.getString(artistCol).orUnknown(AudioItem.UNKNOWN_ARTIST)
                result += AudioItem(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    title = cursor.getString(titleCol) ?: "Unknown",
                    duration = cursor.getLong(durCol),
                    artist = artist,
                    album = cursor.getString(albumCol).orUnknown(AudioItem.UNKNOWN_ALBUM),
                    albumId = albumId,
                    albumArtUri = ContentUris.withAppendedId(albumArtBase, albumId),
                    // TRACK is encoded as disc*1000 + track; keep only the track part.
                    track = cursor.getInt(trackCol) % 1000,
                    year = cursor.getInt(yearCol),
                    dateAddedSeconds = cursor.getLong(dateCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    mimeType = cursor.getString(mimeCol).orEmpty(),
                    relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else "",
                    displayName = cursor.getString(displayCol).orEmpty()
                )
            }
        }
        return result
    }

    /**
     * Loads audio files from a user-picked folder (Storage Access Framework tree Uri).
     * Metadata is read with [MediaMetadataRetriever] since SAF files are not indexed by MediaStore.
     */
    fun loadFromFolder(treeUri: Uri): List<AudioItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val result = ArrayList<AudioItem>()
        for (file in root.listFiles()) {
            if (!file.isFile) continue
            val name = file.name ?: continue
            val isAudio = (file.type?.startsWith("audio") == true) ||
                AUDIO_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
            if (!isAudio) continue
            result += file.toAudioItem(name)
        }
        return result
    }

    private fun DocumentFile.toAudioItem(name: String): AudioItem {
        var duration = 0L
        var artist = AudioItem.UNKNOWN_ARTIST
        var album = AudioItem.UNKNOWN_ALBUM
        var title = name.substringBeforeLast('.')
        var year = 0
        // NOTE: MediaMetadataRetriever only implements AutoCloseable on API 29+, and minSdk is 26,
        // so we release() manually in a finally block instead of using Kotlin's use { }.
        val mmr = MediaMetadataRetriever()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                mmr.setDataSource(pfd.fileDescriptor)
                duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }?.let { artist = it }
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf { it.isNotBlank() }?.let { album = it }
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }?.let { title = it }
                year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.toIntOrNull() ?: 0
            }
        } catch (_: Throwable) {
            // Corrupt/unreadable file — keep the filename-derived defaults.
        } finally {
            try { mmr.release() } catch (_: Throwable) {}
        }
        return AudioItem(
            id = uri.toString().hashCode().toLong(),
            uri = uri,
            title = title,
            duration = duration,
            artist = artist,
            album = album,
            year = year,
            sizeBytes = length(),
            mimeType = type.orEmpty(),
            displayName = name
        )
    }

    private fun String?.orUnknown(fallback: String): String =
        this?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: fallback

    companion object {
        private val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".aac", ".wav", ".flac", ".ogg", ".opus")
    }
}
