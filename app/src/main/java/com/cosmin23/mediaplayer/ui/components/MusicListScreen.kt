package com.cosmin23.mediaplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.elevatedCardElevation
import androidx.compose.material3.CardDefaults.outlinedCardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.PlayerViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow

@Composable
fun MusicListScreen(viewModel: PlayerViewModel) {
    val list = viewModel.audioList.collectAsState().value

    if (list.isEmpty()) {
        // Placeholder when no audio found
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "No audio files found.\nPlease add some to your device and restart.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(list) { item: AudioItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = outlinedCardColors(),
                    elevation = elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.playItem(item) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
