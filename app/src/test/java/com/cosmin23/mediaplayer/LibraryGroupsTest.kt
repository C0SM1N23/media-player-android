package com.cosmin23.mediaplayer

import android.net.Uri
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.toAlbums
import com.cosmin23.mediaplayer.data.toArtists
import com.cosmin23.mediaplayer.data.toFolders
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class LibraryGroupsTest {

    private val uri: Uri = mock(Uri::class.java)

    private fun item(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        track: Int = 0,
        path: String = ""
    ) = AudioItem(
        id = id, uri = uri, title = title, duration = 1000,
        artist = artist, album = album, albumId = albumId, track = track, relativePath = path
    )

    private val library = listOf(
        item(1, "Song B", "Alpha", "First", albumId = 10, track = 2, path = "Music/Rock/"),
        item(2, "Song A", "Alpha", "First", albumId = 10, track = 1, path = "Music/Rock/"),
        item(3, "Solo", "Beta", "Second", albumId = 20, path = "Music/Pop/")
    )

    @Test
    fun `albums group by albumId and order tracks by track number`() {
        val albums = library.toAlbums()
        assertEquals(2, albums.size)
        val first = albums.first { it.id == 10L }
        assertEquals("First", first.title)
        // track 1 (id 2) should come before track 2 (id 1)
        assertEquals(listOf(2L, 1L), first.songs.map { it.id })
    }

    @Test
    fun `artists group by name with album counts`() {
        val artists = library.toArtists()
        assertEquals(2, artists.size)
        val alpha = artists.first { it.name == "Alpha" }
        assertEquals(2, alpha.songCount)
        assertEquals(1, alpha.albumCount)
    }

    @Test
    fun `folders group by relative path`() {
        val folders = library.toFolders()
        assertEquals(2, folders.size)
        assertEquals(setOf("Music/Rock", "Music/Pop"), folders.map { it.path }.toSet())
    }
}
