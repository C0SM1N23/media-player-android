package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.utils.formatDuration


@Composable
fun FavoritesScreen(viewModel: PlayerViewModel) {
    val favoritesSet = viewModel.favorites.collectAsState().value
    val audioList = viewModel.audioList.collectAsState().value

    // Build a list of AudioItem corresponding to favorites (by URI string)
    val favItems = audioList.filter { favoritesSet.contains(it.uri.toString()) }

    if (favItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No favorites yet.\nMark tracks as favorite from the list.", modifier = Modifier.padding(16.dp))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(favItems) { item ->
                FavoriteRow(item = item, onPlay = { viewModel.playItem(item) }, onToggleFav = { viewModel.toggleFavorite(item.uri) })
            }
        }
    }
}

@Composable
fun FavoriteRow(item: AudioItem, onPlay: () -> Unit, onToggleFav: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .clickable { onPlay() }) {

        Row(modifier = Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title)
                Text(formatDuration(item.duration), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = onToggleFav) {
                // Toggles; showing outlined heart for simplicity
                Icon(Icons.Outlined.Favorite, contentDescription = "Remove favorite")
            }
        }
    }
}
