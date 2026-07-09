package com.cosmin23.mediaplayer.data

import android.net.Uri

/** An album: a set of tracks sharing the same albumId. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songs: List<AudioItem>
) {
    val songCount: Int get() = songs.size
}

/** An artist and the tracks/albums they appear on. */
data class Artist(
    val name: String,
    val artworkUri: Uri?,
    val songs: List<AudioItem>
) {
    val songCount: Int get() = songs.size
    val albumCount: Int get() = songs.map { it.albumId }.distinct().size
}

/** A storage folder (derived from the track's relative path). */
data class Folder(
    val path: String,
    val name: String,
    val songs: List<AudioItem>
) {
    val songCount: Int get() = songs.size
}

/** Groups tracks into albums, ordered by album title. Songs within an album are ordered by track no. */
fun List<AudioItem>.toAlbums(): List<Album> =
    groupBy { it.albumId }
        .map { (albumId, tracks) ->
            val first = tracks.first()
            Album(
                id = albumId,
                title = first.album,
                artist = tracks.map { it.artist }.distinct().singleOrNull() ?: "Various artists",
                artworkUri = tracks.firstNotNullOfOrNull { it.albumArtUri },
                songs = tracks.sortedWith(compareBy({ it.track }, { it.title.lowercase() }))
            )
        }
        .sortedBy { it.title.lowercase() }

/** Groups tracks by artist, ordered by name. */
fun List<AudioItem>.toArtists(): List<Artist> =
    groupBy { it.artist }
        .map { (name, tracks) ->
            Artist(
                name = name,
                artworkUri = tracks.firstNotNullOfOrNull { it.albumArtUri },
                songs = tracks.sortedBy { it.title.lowercase() }
            )
        }
        .sortedBy { it.name.lowercase() }

/** Groups tracks by folder (relative path). Tracks without a path fall under "Other". */
fun List<AudioItem>.toFolders(): List<Folder> =
    groupBy { it.relativePath.trim('/').ifBlank { "Other" } }
        .map { (path, tracks) ->
            Folder(
                path = path,
                name = path.trimEnd('/').substringAfterLast('/').ifBlank { path },
                songs = tracks.sortedBy { it.title.lowercase() }
            )
        }
        .sortedBy { it.name.lowercase() }
