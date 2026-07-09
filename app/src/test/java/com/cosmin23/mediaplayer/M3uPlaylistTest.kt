package com.cosmin23.mediaplayer

import com.cosmin23.mediaplayer.data.M3uPlaylist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistTest {

    @Test
    fun `parseReferences skips comments and blank lines`() {
        val content = """
            #EXTM3U
            #EXTINF:180,Artist - Song One
            song one.mp3

            #EXTINF:200,Artist - Song Two
            folder/song two.mp3
        """.trimIndent()

        val refs = M3uPlaylist.parseReferences(content)
        assertEquals(listOf("song one.mp3", "folder/song two.mp3"), refs)
    }

    @Test
    fun `fileNameOf strips directories`() {
        assertEquals("song.mp3", M3uPlaylist.fileNameOf("/storage/Music/song.mp3"))
        assertEquals("song.mp3", M3uPlaylist.fileNameOf("Music\\song.mp3"))
        assertEquals("song.mp3", M3uPlaylist.fileNameOf("song.mp3"))
    }

    @Test
    fun `export writes header and one EXTINF per entry`() {
        val entries = listOf(
            M3uPlaylist.M3uEntry("song.mp3", "Title", "Artist", 180_000),
            M3uPlaylist.M3uEntry("song2.mp3", "Title2", "Artist2", 200_000)
        )
        val output = M3uPlaylist.export(entries)
        assertTrue(output.startsWith("#EXTM3U"))
        assertEquals(2, Regex("#EXTINF").findAll(output).count())
        assertTrue(output.contains("#EXTINF:180,Artist - Title"))
        assertTrue(output.contains("song2.mp3"))
    }
}
