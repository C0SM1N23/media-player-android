package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { viewModel.play() }) {
            Text("Play")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.stop() }) {
            Text("Stop")
        }
    }
}
