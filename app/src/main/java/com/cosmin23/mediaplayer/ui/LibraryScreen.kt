package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.Album
import com.cosmin23.mediaplayer.data.Artist
import com.cosmin23.mediaplayer.data.Folder
import androidx.compose.material3.SnackbarHostState
import com.cosmin23.mediaplayer.ui.components.AlbumArt
import com.cosmin23.mediaplayer.ui.components.SongList

private val LIBRARY_TABS = listOf("Songs", "Albums", "Artists", "Folders")

/** Library with Songs / Albums / Artists / Folders tabs. Grouped tabs open the collection screen. */
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        TabRow(selectedTabIndex = tab) {
            LIBRARY_TABS.forEachIndexed { index, label ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
            }
        }
        when (tab) {
            0 -> SongsTab(viewModel, snackbarHostState)
            1 -> AlbumsTab(viewModel, onOpenCollection)
            2 -> ArtistsTab(viewModel, onOpenCollection)
            else -> FoldersTab(viewModel, onOpenCollection)
        }
    }
}

@Composable
private fun SongsTab(viewModel: PlayerViewModel, snackbarHostState: SnackbarHostState) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsState()

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        if (songs.isEmpty()) {
            EmptyLibrary(if (query.isBlank()) "No music found on this device" else "No matches")
        } else {
            SongList(
                songs = songs,
                onPlay = { index -> viewModel.play(songs, index) },
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun AlbumsTab(viewModel: PlayerViewModel, onOpenCollection: () -> Unit) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    if (albums.isEmpty()) { EmptyLibrary("No albums"); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCell(album) {
                viewModel.openCollection(album.title, "${album.artist} • ${album.songCount} songs", album.songs)
                onOpenCollection()
            }
        }
    }
}

@Composable
private fun AlbumCell(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AlbumArt(model = album.artworkUri, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 16)
        Spacer(Modifier.height(6.dp))
        Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArtistsTab(viewModel: PlayerViewModel, onOpenCollection: () -> Unit) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    if (artists.isEmpty()) { EmptyLibrary("No artists"); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            RowTile(
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                title = artist.name,
                subtitle = "${artist.songCount} songs • ${artist.albumCount} albums"
            ) {
                viewModel.openCollection(artist.name, "${artist.songCount} songs", artist.songs)
                onOpenCollection()
            }
        }
    }
}

@Composable
private fun FoldersTab(viewModel: PlayerViewModel, onOpenCollection: () -> Unit) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    if (folders.isEmpty()) { EmptyLibrary("No folders"); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            RowTile(
                icon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                title = folder.name,
                subtitle = "${folder.songCount} songs"
            ) {
                viewModel.openCollection(folder.name, folder.path, folder.songs)
                onOpenCollection()
            }
        }
    }
}

@Composable
private fun RowTile(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyLibrary(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
