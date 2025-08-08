package com.cosmin23.mediaplayer.ui

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(audioSessionId: Int) {
    if (audioSessionId <= 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Equalizer unavailable — no active audio session.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // Hold the Equalizer instance
    val equalizerRemember = remember(audioSessionId) {
        try {
            Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (t: Throwable) {
            null
        }
    }

    DisposableEffect(equalizerRemember) {
        onDispose {
            try {
                equalizerRemember?.release()
            } catch (_: Throwable) { }
        }
    }

    val eq = equalizerRemember
    if (eq == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not initialize Equalizer (device may not support it).", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val numBands = eq.numberOfBands.toInt()
    val bandLevelRange = eq.bandLevelRange // shortArray [min, max]
    val minLevel = bandLevelRange[0].toInt()
    val maxLevel = bandLevelRange[1].toInt()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)) {

        Text("Equalizer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Text("Session ID: $audioSessionId", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        // For each band show a slider
        for (band in 0 until numBands) {
            val bandIndex = band.toShort()
            val centerFreq = eq.getCenterFreq(bandIndex) / 1000 // in Hz (kHz-ish)
            val currentLevel = remember { mutableStateOf(eq.getBandLevel(bandIndex).toInt()) }

            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Band ${band + 1} • ${centerFreq} Hz")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatLevel(currentLevel.value), modifier = Modifier.width(48.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = (currentLevel.value - minLevel).toFloat(),
                            onValueChange = { f ->
                                val intVal = (f).roundToInt() + minLevel
                                currentLevel.value = intVal
                                try {
                                    eq.setBandLevel(bandIndex, intVal.toShort())
                                } catch (_: Throwable) { }
                            },
                            valueRange = 0f..(maxLevel - minLevel).toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatLevel(level: Int): String {
    // level in millibels typically [-1500 .. 1500]
    val db = level / 100.0
    return String.format("%.1f dB", db)
}
