package com.cosmin23.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import kotlinx.coroutines.launch

/**
 * The one list used everywhere songs are shown (library, favorites, album/artist/folder detail,
 * playlists). Centralises swipe-to-favorite / swipe-to-queue with haptics + undo snackbars, the
 * long-press context menu, add-to-playlist, and an optional A-Z fast-scroll rail.
 */
@Composable
fun SongList(
    songs: List<AudioItem>,
    onPlay: (index: Int) -> Unit,
    viewModel: PlayerViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    fastScroll: Boolean = songs.size >= 40,
    onRemove: ((AudioItem) -> Unit)? = null
) {
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var menuFor by remember { mutableStateOf<AudioItem?>(null) }
    var addToPlaylistFor by remember { mutableStateOf<AudioItem?>(null) }
    var createPlaylistForSong by remember { mutableStateOf<AudioItem?>(null) }

    fun snack(message: String, undo: (() -> Unit)? = null) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (undo != null) "Undo" else null
            )
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (header != null) item(key = "header") { header() }

            itemsIndexed(songs, key = { _, item -> item.id }) { index, item ->
                val isFav = item.id in favorites
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFavorite(item.id)
                                snack(
                                    if (isFav) "Removed from favorites" else "Added to favorites",
                                    undo = { viewModel.toggleFavorite(item.id) }
                                )
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.addToQueue(item)
                                snack("Added to queue")
                            }
                            else -> Unit
                        }
                        false // never actually dismiss the row — snap back after the action
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { SwipeBackground(dismissState.targetValue, isFav) }
                ) {
                    SongListItem(
                        item = item,
                        isFavorite = isFav,
                        isCurrent = nowPlaying?.id == item.id,
                        onClick = { onPlay(index) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuFor = item
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) }
                    )
                }
            }
        }

        if (fastScroll && songs.isNotEmpty()) {
            AlphabetRail(
                letters = remember(songs) { songs.map { it.sortLetter }.distinct() },
                onLetter = { letter ->
                    val target = songs.indexOfFirst { it.sortLetter == letter }
                    if (target >= 0) scope.launch { listState.scrollToItem(target + if (header != null) 1 else 0) }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }

    // Context menu
    menuFor?.let { song ->
        val isFav = song.id in favorites
        val actions = buildList {
            add(SongAction("Play next", Icons.AutoMirrored.Filled.PlaylistPlay) { viewModel.playNext(song) })
            add(SongAction("Add to queue", Icons.AutoMirrored.Filled.QueueMusic) { viewModel.addToQueue(song) })
            add(SongAction("Add to playlist", Icons.AutoMirrored.Filled.PlaylistAdd) { addToPlaylistFor = song })
            add(
                SongAction(
                    if (isFav) "Remove from favorites" else "Add to favorites",
                    if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                ) { viewModel.toggleFavorite(song.id) }
            )
            if (onRemove != null) {
                add(SongAction("Remove from playlist", Icons.Filled.Delete) { onRemove(song) })
            }
        }
        SongContextSheet(song = song, actions = actions, onDismiss = { menuFor = null })
    }

    // Add to playlist
    addToPlaylistFor?.let { song ->
        AddToPlaylistSheet(
            playlists = playlists,
            onCreateNew = { createPlaylistForSong = song; addToPlaylistFor = null },
            onPick = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                snack("Added to playlist")
            },
            onDismiss = { addToPlaylistFor = null }
        )
    }

    // New playlist (from add-to-playlist)
    createPlaylistForSong?.let { song ->
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                viewModel.createPlaylist(name) { newId -> viewModel.addSongToPlaylist(newId, song.id) }
                snack("Playlist created")
            },
            onDismiss = { createPlaylistForSong = null }
        )
    }
}

@Composable
private fun SwipeBackground(target: SwipeToDismissBoxValue, isFavorite: Boolean) {
    val (color, alignment, icon) = when (target) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.colorScheme.primaryContainer, Alignment.CenterStart,
            if (isFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            MaterialTheme.colorScheme.secondaryContainer, Alignment.CenterEnd,
            Icons.AutoMirrored.Filled.QueueMusic
        )
        else -> Triple(Color.Transparent, Alignment.Center, Icons.Filled.PlayArrow)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (target != SwipeToDismissBoxValue.Settled) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun AlphabetRail(
    letters: List<String>,
    onLetter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        letters.forEach { letter ->
            Text(
                text = letter,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onLetter(letter) }
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}
