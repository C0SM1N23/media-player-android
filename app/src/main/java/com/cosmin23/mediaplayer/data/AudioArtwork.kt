package com.cosmin23.mediaplayer.data

import android.net.Uri

/**
 * Coil load model for a track's artwork. Carries both the MediaStore album-art uri (fast path) and
 * the track's own uri (fallback), letting the custom fetcher recover art on devices where the
 * album-art uri returns nothing but the file still has embedded artwork.
 */
data class AudioArtwork(
    val albumArtUri: Uri?,
    val mediaUri: Uri
)

/** Convenience accessor used everywhere artwork is displayed. */
val AudioItem.artwork: AudioArtwork
    get() = AudioArtwork(albumArtUri = albumArtUri, mediaUri = uri)
