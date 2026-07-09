package com.cosmin23.mediaplayer

import android.net.Uri
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class SortOrderTest {

    private val uri: Uri = mock(Uri::class.java)

    private fun item(
        id: Long,
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        duration: Long = 1000,
        dateAdded: Long = 0
    ) = AudioItem(
        id = id, uri = uri, title = title, duration = duration,
        artist = artist, album = album, dateAddedSeconds = dateAdded
    )

    private val library = listOf(
        item(1, "Banana", artist = "Zed", duration = 300, dateAdded = 10),
        item(2, "apple", artist = "Alpha", duration = 100, dateAdded = 30),
        item(3, "Cherry", artist = "Mike", duration = 200, dateAdded = 20)
    )

    @Test
    fun `title sort is case-insensitive alphabetical`() {
        val titles = SortOrder.TITLE.sorted(library).map { it.title }
        assertEquals(listOf("apple", "Banana", "Cherry"), titles)
    }

    @Test
    fun `duration sort is descending`() {
        val ids = SortOrder.DURATION.sorted(library).map { it.id }
        assertEquals(listOf(1L, 3L, 2L), ids)
    }

    @Test
    fun `date added sort is newest first`() {
        val ids = SortOrder.DATE_ADDED.sorted(library).map { it.id }
        assertEquals(listOf(2L, 3L, 1L), ids)
    }

    @Test
    fun `artist sort orders by artist name`() {
        val artists = SortOrder.ARTIST.sorted(library).map { it.artist }
        assertEquals(listOf("Alpha", "Mike", "Zed"), artists)
    }

    @Test
    fun `fromName falls back to title`() {
        assertEquals(SortOrder.TITLE, SortOrder.fromName("nonsense"))
        assertEquals(SortOrder.ALBUM, SortOrder.fromName("ALBUM"))
    }
}
