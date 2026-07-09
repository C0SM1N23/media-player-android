package com.cosmin23.mediaplayer.data

/**
 * Sort options for the song library. Applied in-memory after loading so switching is instant
 * and does not re-query MediaStore.
 */
enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Recently added");

    fun sorted(items: List<AudioItem>): List<AudioItem> = when (this) {
        TITLE -> items.sortedBy { it.title.lowercase() }
        ARTIST -> items.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        ALBUM -> items.sortedWith(compareBy({ it.album.lowercase() }, { it.track }, { it.title.lowercase() }))
        DURATION -> items.sortedByDescending { it.duration }
        DATE_ADDED -> items.sortedByDescending { it.dateAddedSeconds }
    }

    companion object {
        fun fromName(name: String?): SortOrder =
            entries.firstOrNull { it.name == name } ?: TITLE
    }
}
