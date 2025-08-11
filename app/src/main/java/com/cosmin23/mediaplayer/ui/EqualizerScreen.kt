package com.cosmin23.mediaplayer.ui

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(viewModel: PlayerViewModel) {
    val sessionId by viewModel.audioSessionId.collectAsState()

    // Dacă nu există sesiune audio activă, arăt un mesaj prietenos
    if (sessionId <= 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Equalizer unavailable — start playback first to enable the device equalizer.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // Creăm Equalizer pentru sesiunea curentă; recreate când sessionId se schimbă
    val eq: Equalizer? = remember(sessionId) {
        try {
            Equalizer(0, sessionId).apply { enabled = true }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    // Eliberăm EQ la dispose
    DisposableEffect(eq) {
        onDispose { try { eq?.release() } catch (_: Throwable) {} }
    }

    if (eq == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Device does not support Equalizer.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    // Date despre benzi
    val numBands = eq.numberOfBands.toInt()
    val bandRange = eq.bandLevelRange // shortArray [min, max] in millibels
    val minLevel = bandRange[0].toInt()
    val maxLevel = bandRange[1].toInt()

    // Numele preset-urilor sistem (dacă există)
    val systemPresets = remember(eq) {
        val count = eq.numberOfPresets.toInt()
        (0 until count).map { idx -> eq.getPresetName(idx.toShort()) }
    }

    // Template-uri simple (db values, ex: 5 = +5dB). Vor fi mapate la numBands:
    val templates = mapOf(
        "Rock" to listOf(5, 3, 0, 3, 5),
        "Pop" to listOf(0, 4, 5, 4, 0),
        "Jazz" to listOf(4, 2, 0, 2, 4)
    )

    // Helper: mapează un template (în dB) la numarul actual de benzi
    fun mapTemplateToBands(templateDb: List<Int>, bands: Int): List<Int> {
        if (bands <= 0) return emptyList()
        if (bands == templateDb.size) return templateDb
        val tSize = templateDb.size
        if (tSize == 1) return List(bands) { templateDb[0] }
        return List(bands) { i ->
            if (bands == 1) templateDb[0]
            else {
                val pos = i.toDouble() * (tSize - 1) / (bands - 1)
                val idx = pos.roundToInt().coerceIn(0, tSize - 1)
                templateDb[idx]
            }
        }
    }

    // Aplică o listă de valori în millibels pe EQ; lista trebuie să aibă length == numBands
    fun applyLevelsMillis(levels: List<Int>) {
        for (i in 0 until numBands) {
            val clamped = levels[i].coerceIn(minLevel, maxLevel)
            try {
                eq.setBandLevel(i.toShort(), clamped.toShort())
            } catch (_: Throwable) { }
        }
    }

    // Starea locală a nivelurilor (în millibels)
    var bandLevels by remember(eq) {
        mutableStateOf(List(numBands) { b -> eq.getBandLevel(b.toShort()).toInt() })
    }

    // Preset activ (nume). Inițial, dacă sistemul are preseturi, luăm primul, altfel "Custom"
    var activePreset by remember { mutableStateOf(if (systemPresets.isNotEmpty()) systemPresets.first() else "Custom") }

    // Functie de încărcare preset (poate fi system sau template sau custom)
    fun loadPresetByName(name: String) {
        when {
            templates.containsKey(name) -> {
                val mappedDb = mapTemplateToBands(templates[name]!!, numBands) // dB list
                val millis = mappedDb.map { it * 100 } // convert dB -> millibels (Short)
                val clamped = millis.map { it.coerceIn(minLevel, maxLevel) }
                applyLevelsMillis(clamped)
                bandLevels = clamped
                activePreset = name
            }
            systemPresets.contains(name) -> {
                val idx = systemPresets.indexOf(name)
                try {
                    eq.usePreset(idx.toShort())
                    // citim valorile curente
                    bandLevels = List(numBands) { b -> eq.getBandLevel(b.toShort()).toInt() }
                } catch (_: Throwable) { }
                activePreset = name
            }
            name == "Flat" -> {
                val flat = List(numBands) { 0 }
                val flatMillis = flat.map { it.coerceIn(minLevel, maxLevel) }
                applyLevelsMillis(flatMillis)
                bandLevels = flatMillis
                activePreset = "Custom"
            }
            else -> {
                // custom: păstrăm valorile curente
                activePreset = "Custom"
            }
        }
    }

    // Prima incarcarea: sincronizeaza starea cu EQ-ul real
    LaunchedEffect(eq) {
        bandLevels = List(numBands) { b -> eq.getBandLevel(b.toShort()).toInt() }
    }

    // UI
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Equalizer", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Text("Presets", style = MaterialTheme.typography.titleMedium)
        val shownPresets = remember(systemPresets) {
            (systemPresets + templates.keys + listOf("Flat", "Custom")).distinct()
        }
        LazyRow(Modifier.padding(vertical = 8.dp)) {
            items(shownPresets) { name ->
                FilterChip(
                    selected = (activePreset == name),
                    onClick = { loadPresetByName(name) },
                    label = { Text(name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Bands", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        // fiecare banda cu slider
        for (band in 0 until numBands) {
            val bandIndex = band.toShort()
            val centerFreq = try { eq.getCenterFreq(bandIndex) / 1000 } catch (_: Throwable) { 0 }
            val level = bandLevels.getOrElse(band) { 0 }
            val normalized = (level - minLevel).toFloat() / (maxLevel - minLevel).toFloat()

            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${centerFreq} Hz", Modifier.width(100.dp))
                    Spacer(Modifier.weight(1f))
                    Text(String.format("%.1f dB", level / 100.0))
                }
                Slider(
                    value = normalized.coerceIn(0f, 1f),
                    onValueChange = { f ->
                        // când utilizator muta sliderul => intram in Custom
                        if (activePreset != "Custom") activePreset = "Custom"
                        val newLevel = (minLevel + (f * (maxLevel - minLevel))).roundToInt()
                        // aplicare live
                        try { eq.setBandLevel(bandIndex, newLevel.toShort()) } catch (_: Throwable) {}
                        bandLevels = bandLevels.toMutableList().also { it[band] = newLevel }
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { loadPresetByName("Flat") }) { Text("Flat") }
            Button(onClick = { /* redundantly apply current bandLevels again */ applyLevelsMillis(bandLevels) }) {
                Text("Apply (confirm)")
            }
            Button(onClick = {
                // read system again
                bandLevels = List(numBands) { b -> eq.getBandLevel(b.toShort()).toInt() }
                activePreset = "Custom"
            }) {
                Text("Refresh")
            }
        }
    }
}
