package com.cosmin23.mediaplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop

@Composable
fun PlayerControls(viewModel: PlayerViewModel) {
    val positionState = viewModel.position.collectAsState()
    val durationState = viewModel.duration.collectAsState()
    val position = positionState.value
    val duration = durationState.value

    val isPlaying = viewModel.playingUri.collectAsState().value != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = position.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.pause() }, enabled = isPlaying) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
            IconButton(onClick = { viewModel.stop() }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
            }
        }
    }
}
