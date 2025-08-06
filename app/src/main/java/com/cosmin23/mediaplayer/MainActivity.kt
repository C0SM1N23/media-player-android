package com.cosmin23.mediaplayer

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cosmin23.mediaplayer.ui.components.MusicListScreen
import com.cosmin23.mediaplayer.ui.components.PlayerControls
import com.cosmin23.mediaplayer.ui.theme.MediaPlayerTheme

class MainActivity : ComponentActivity() {
    private val requestReadPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestReadPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

        setContent {
            MediaPlayerTheme {
                val vm: PlayerViewModel = viewModel()
                Column(modifier = Modifier.fillMaxSize()) {
                    MusicListScreen(viewModel = vm)
                    PlayerControls(viewModel = vm)
                }
            }
        }
    }
}
