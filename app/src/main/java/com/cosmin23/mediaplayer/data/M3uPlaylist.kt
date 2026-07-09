package com.cosmin23.mediaplayer.data

/**
 * Minimal M3U/M3U8 read + write support for importing and exporting playlists.
 *
 * Export writes an extended M3U (`#EXTM3U` + `#EXTINF` metadata + one path per song). Import parses
 * the non-comment lines and returns the referenced file references; matching those references back
 * to library tracks is done by the caller (by file name / path), so this stays pure and testable.
 */
object M3uPlaylist {

    fun export(entries: List<M3uEntry>): String = buildString {
        appendLine("#EXTM3U")
        entries.forEach { entry ->
            val seconds = (entry.durationMs / 1000).coerceAtLeast(0)
            appendLine("#EXTINF:$seconds,${entry.artist} - ${entry.title}")
            appendLine(entry.reference)
        }
    }

    /** Returns the ordered list of file references (paths or uris), ignoring comments/blank lines. */
    fun parseReferences(content: String): List<String> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

    /** The bare file name of a reference, used to match against the on-device library. */
    fun fileNameOf(reference: String): String =
        reference.substringAfterLast('/').substringAfterLast('\\')

    data class M3uEntry(
        val reference: String,
        val title: String,
        val artist: String,
        val durationMs: Long
    )
}
