package com.cosmin23.mediaplayer.data

/** A named, ordered set of tracks shown in the generic collection detail screen. */
data class SongCollection(
    val title: String,
    val subtitle: String,
    val songs: List<AudioItem>
)
