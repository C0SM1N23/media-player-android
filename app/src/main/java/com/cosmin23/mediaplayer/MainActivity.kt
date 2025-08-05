package com.cosmin23.mediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cosmin23.mediaplayer.ui.PlayerScreen
import com.cosmin23.mediaplayer.ui.theme.MediaPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaPlayerTheme {
                val playerViewModel: PlayerViewModel = viewModel()
                PlayerScreen(viewModel = playerViewModel)
            }
        }
    }
}
