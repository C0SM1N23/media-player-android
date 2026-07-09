package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.ui.components.AlbumArt

/** Landing screen: quick access to the smart playlists plus artwork carousels of what to play. */
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyPlayed: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Your music",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile("Favorites", Icons.Filled.Favorite, Modifier.weight(1f), onOpenFavorites)
                QuickTile("Recent", Icons.Filled.History, Modifier.weight(1f), onOpenRecentlyPlayed)
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile("Most played", Icons.Filled.Whatshot, Modifier.weight(1f), onOpenMostPlayed)
                QuickTile("New", Icons.Filled.NewReleases, Modifier.weight(1f), onOpenRecentlyAdded)
            }
        }

        carousel("Recently played", recentlyPlayed, viewModel)
        carousel("Most played", mostPlayed, viewModel)
        carousel("Recently added", recentlyAdded, viewModel)
        carousel("Favorites", favorites, viewModel)

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.carousel(
    title: String,
    songs: List<AudioItem>,
    viewModel: PlayerViewModel
) {
    if (songs.isEmpty()) return
    item(key = "title_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )
    }
    item(key = "row_$title") {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                SongCard(song = song, onClick = { viewModel.play(songs, index) })
            }
        }
    }
}

@Composable
private fun SongCard(song: AudioItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        AlbumArt(
            item = song,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 16
        )
        Spacer(Modifier.height(6.dp))
        Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickTile(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
