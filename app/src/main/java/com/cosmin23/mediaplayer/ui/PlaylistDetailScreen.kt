package com.cosmin23.mediaplayer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.artwork
import com.cosmin23.mediaplayer.ui.components.ConfirmDialog
import com.cosmin23.mediaplayer.ui.components.DraggableItem
import com.cosmin23.mediaplayer.ui.components.PlaylistNameDialog
import com.cosmin23.mediaplayer.ui.components.SongListItem
import com.cosmin23.mediaplayer.ui.components.dragContainer
import com.cosmin23.mediaplayer.ui.components.rememberDragDropState
import androidx.compose.foundation.lazy.LazyColumn

/**
 * Playlist detail with drag-to-reorder (long-press a row), plus rename / delete / export from the
 * toolbar. Reordering is applied locally for smooth UX and committed to Room when the drag ends.
 */
@Composable
fun PlaylistDetailScreen(
    viewModel: PlayerViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name by viewModel.playlistName(playlistId).collectAsStateWithLifecycle("Playlist")
    val songs by viewModel.playlistSongs(playlistId).collectAsStateWithLifecycle(emptyList())
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val localSongs = remember { mutableStateListOf<AudioItem>() }
    var pendingCommit by remember { mutableStateOf(false) }

    val dragState = rememberDragDropState(listState) { from, to ->
        if (from in localSongs.indices && to in localSongs.indices) {
            localSongs.add(to, localSongs.removeAt(from))
            pendingCommit = true
        }
    }

    // Mirror the persisted list into the local one whenever it changes and we're not mid-drag.
    LaunchedEffect(songs) {
        if (dragState.draggingItemIndex == null) {
            localSongs.clear()
            localSongs.addAll(songs)
        }
    }
    // Persist the new order once the drag finishes.
    LaunchedEffect(dragState.draggingItemIndex) {
        if (dragState.draggingItemIndex == null && pendingCommit) {
            viewModel.reorderPlaylist(playlistId, localSongs.map { it.id })
            pendingCommit = false
        }
    }

    var menu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri -> uri?.let { viewModel.exportPlaylistM3u(playlistId, it) } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; showRename = true })
                        DropdownMenuItem(
                            text = { Text("Export (.m3u)") },
                            onClick = { menu = false; exportLauncher.launch("${name}.m3u") }
                        )
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; showDelete = true })
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CollectionHeader(
                title = name,
                subtitle = "${localSongs.size} songs",
                artworkModel = localSongs.firstOrNull()?.artwork,
                onPlayAll = { viewModel.play(localSongs, 0) },
                onShuffle = { viewModel.shufflePlay(localSongs) }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().dragContainer(dragState)
            ) {
                itemsIndexed(localSongs, key = { index, _ -> index }) { index, item ->
                    DraggableItem(dragState, index) {
                        SongListItem(
                            item = item,
                            isFavorite = false,
                            isCurrent = nowPlaying?.id == item.id,
                            onClick = { viewModel.play(localSongs, index) },
                            dragHandle = Modifier
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = name,
            onConfirm = { viewModel.renamePlaylist(playlistId, it) },
            onDismiss = { showRename = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete playlist",
            message = "Delete \"$name\"? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deletePlaylist(playlistId); onBack() },
            onDismiss = { showDelete = false }
        )
    }
}
