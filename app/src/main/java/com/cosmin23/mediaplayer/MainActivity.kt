package com.cosmin23.mediaplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cosmin23.mediaplayer.ui.PlayerScreen
import com.cosmin23.mediaplayer.ui.EqualizerScreen
import com.cosmin23.mediaplayer.ui.FavoritesScreen
import com.cosmin23.mediaplayer.ui.SettingsScreen
import com.cosmin23.mediaplayer.ui.components.TopBarWithMenu
import com.cosmin23.mediaplayer.ui.theme.MediaPlayerTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    // permisiune citire audio (READ_MEDIA_AUDIO pentru Android 13+)
    private val readPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Permission denied — audio list may be empty.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                vm.loadAudio()
            }
        }

    // permisiune notifications (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Notification permission denied — foreground notification won't show.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestStoragePermissionIfNeeded()
        requestNotificationPermissionIfNeeded()

        setContent {
            MediaPlayerTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: "player"

                Scaffold(
                    topBar = {
                        TopBarWithMenu(
                            title = when (currentRoute) {
                                "player" -> "Player"
                                "equalizer" -> "Equalizer"
                                "favorites" -> "Favorites"
                                "settings" -> "Settings"
                                else -> "MediaPlayer"
                            },
                            onMenuClick = {
                                // simplu: navigăm la Settings când se apasă butonul de meniu
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                ) { innerPadding: PaddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "player",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // folosește PlayerScreen ca ecran principal (lista + controls pot fi integrate acolo)
                        composable("player") {
                            PlayerScreen(viewModel = vm)
                        }

                        // equalizer: momentan îi trimitem 0; poți expune audioSessionId din VM
                        composable("equalizer") {
                            EqualizerScreen(audioSessionId = 0)
                        }

                        composable("favorites") {
                            FavoritesScreen(viewModel = vm)
                        }

                        composable("settings") {
                            SettingsScreen(
                                navController = null,
                                onBack = { navController.popBackStack() },
                                onMusicListClicked = { navController.navigate("player") },
                                onEqualizerClicked = { navController.navigate("equalizer") },
                                onFavoritesClicked = { navController.navigate("favorites") }
                            )
                        }
                    }
                }
            }
        }

        // Observăm playingUri pentru a porni/stopa PlayerService
        lifecycleScope.launch {
            vm.playingUri.collectLatest { uri ->
                if (uri != null) {
                    val intent = Intent(this@MainActivity, PlayerService::class.java).apply {
                        action = PlayerService.ACTION_PLAY
                        putExtra(PlayerService.EXTRA_URI, uri.toString())
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this@MainActivity, intent)
                    } else {
                        startService(intent)
                    }
                } else {
                    val intent = Intent(this@MainActivity, PlayerService::class.java).apply {
                        action = PlayerService.ACTION_STOP
                    }
                    startService(intent)
                }
            }
        }

        // tratează intentul la pornire (de ex. share/open-uri)
        handleIncomingIntentIfAny(intent)
    }

    // semnătura corectă pentru onNewIntent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntentIfAny(intent)
    }

    private fun handleIncomingIntentIfAny(intent: Intent?) {
        intent?.data?.let { uri ->
            // ViewModel trebuie să implementeze playUriFromExternal(uri)
            vm.playUriFromExternal(uri)
            // opțional: poți naviga la player când vine un URI extern
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val granted = ContextCompat.checkSelfPermission(this, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            readPermissionLauncher.launch(permission)
        } else {
            vm.loadAudio()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
