package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.playback.AudioEffects
import java.util.Locale

/**
 * Audio effects screen backed by [AudioEffects] (effects live with the player, so changes persist
 * across navigation and restarts). Provides an enable toggle, device presets, per-band vertical
 * sliders, and bass-boost / virtualizer controls.
 */
@Composable
fun EqualizerScreen(viewModel: PlayerViewModel) {
    val sessionId by viewModel.audioSessionId.collectAsStateWithLifecycle()

    if (sessionId <= 0 || !AudioEffects.isAvailable) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Play a track to enable audio effects.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    var enabled by remember { mutableStateOf(AudioEffects.isEnabled) }
    var bands by remember { mutableStateOf(AudioEffects.currentBands()) }
    var bass by remember { mutableStateOf(AudioEffects.bassStrength) }
    var virtualizer by remember { mutableStateOf(AudioEffects.virtualizerStrength) }
    val presets = remember { AudioEffects.presetNames }
    val minLevel = AudioEffects.minLevel
    val maxLevel = AudioEffects.maxLevel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Equalizer", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (enabled) "On" else "Off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    AudioEffects.isEnabled = it
                }
            )
        }

        if (presets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, name ->
                    FilterChip(
                        selected = false,
                        enabled = enabled,
                        onClick = {
                            AudioEffects.usePreset(index)
                            bands = AudioEffects.currentBands()
                        },
                        label = { Text(name) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bands.indices.forEach { band ->
                    EqBand(
                        freqHz = AudioEffects.centerFreq(band),
                        level = bands[band],
                        minLevel = minLevel,
                        maxLevel = maxLevel,
                        enabled = enabled,
                        onLevel = { newLevel ->
                            AudioEffects.setBandLevel(band, newLevel)
                            bands = bands.toMutableList().also { it[band] = newLevel }
                        }
                    )
                }
            }
        }

        if (AudioEffects.bassSupported) {
            Spacer(Modifier.height(20.dp))
            EffectSlider(
                title = "Bass boost",
                value = bass,
                enabled = enabled,
                onValue = { bass = it; AudioEffects.bassStrength = it }
            )
        }
        if (AudioEffects.virtualizerSupported) {
            Spacer(Modifier.height(12.dp))
            EffectSlider(
                title = "Virtualizer",
                value = virtualizer,
                enabled = enabled,
                onValue = { virtualizer = it; AudioEffects.virtualizerStrength = it }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EqBand(
    freqHz: Int,
    level: Int,
    minLevel: Int,
    maxLevel: Int,
    enabled: Boolean,
    onLevel: (Int) -> Unit
) {
    val normalized = if (maxLevel > minLevel) (level - minLevel).toFloat() / (maxLevel - minLevel) else 0.5f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%+.0f", level / 100.0),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier.height(160.dp).width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = normalized.coerceIn(0f, 1f),
                onValueChange = { f -> onLevel((minLevel + f * (maxLevel - minLevel)).toInt()) },
                enabled = enabled,
                modifier = Modifier
                    .requiredWidth(160.dp)
                    .rotate(-90f)
            )
        }
        Text(
            text = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EffectSlider(
    title: String,
    value: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text("${value / 10}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValue(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled
        )
    }
}
