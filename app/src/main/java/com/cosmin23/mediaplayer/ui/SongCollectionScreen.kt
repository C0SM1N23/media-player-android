package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.artwork
import com.cosmin23.mediaplayer.ui.components.AlbumArt
import com.cosmin23.mediaplayer.ui.components.SongList

/**
 * Generic "list of songs with a header" screen reused by album / artist / folder / favorites and
 * the smart playlists. The reorder-capable playlist detail is separate.
 */
@Composable
fun SongCollectionScreen(
    title: String,
    subtitle: String,
    songs: List<AudioItem>,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No songs here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SongList(
                songs = songs,
                onPlay = { index -> viewModel.play(songs, index) },
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(padding),
                header = {
                    CollectionHeader(
                        title = title,
                        subtitle = subtitle,
                        artworkModel = songs.firstOrNull()?.artwork,
                        onPlayAll = { viewModel.play(songs, 0) },
                        onShuffle = { viewModel.shufflePlay(songs) }
                    )
                }
            )
        }
    }
}

@Composable
fun CollectionHeader(
    title: String,
    subtitle: String,
    artworkModel: Any?,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(model = artworkModel, modifier = Modifier.size(96.dp), cornerRadius = 16)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play")
            }
            OutlinedButton(onClick = onShuffle, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Shuffle")
            }
        }
    }
}
