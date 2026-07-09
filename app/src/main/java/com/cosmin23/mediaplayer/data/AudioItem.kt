package com.cosmin23.mediaplayer.data

import android.net.Uri

/**
 * A single playable audio track.
 *
 * Backward compatible with the original model (`id`, `uri`, `title`, `duration` are all still
 * present) but enriched with the metadata a modern player needs: artist, album, artwork,
 * track/year, folder and file information.
 */
data class AudioItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val duration: Long,
    val artist: String = UNKNOWN_ARTIST,
    val album: String = UNKNOWN_ALBUM,
    val albumId: Long = 0L,
    val albumArtUri: Uri? = null,
    val track: Int = 0,
    val year: Int = 0,
    val dateAddedSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "",
    val relativePath: String = "",
    val displayName: String = ""
) {
    /** First letter used by the alphabetical fast-scroll index. */
    val sortLetter: String
        get() = title.trim().firstOrNull()
            ?.takeIf { it.isLetter() }
            ?.uppercaseChar()
            ?.toString()
            ?: "#"

    companion object {
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}
