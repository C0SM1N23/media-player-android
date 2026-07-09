package com.cosmin23.mediaplayer.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer

/**
 * Owns the platform audio effects (Equalizer, Bass Boost, Virtualizer) attached to the player's
 * audio session for the lifetime of [MusicService]. Because the effects live with the player (not
 * with the UI), the equalizer keeps affecting playback after you leave the screen, and the settings
 * are persisted so they survive restarts.
 *
 * `Virtualizer` is deprecated on API 34+ (superseded by Spatializer) but remains functional and is
 * still the appropriate control for a music-player equalizer, so the deprecation is suppressed.
 */
@Suppress("DEPRECATION")
object AudioEffects {

    private var prefs: SharedPreferences? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    var sessionId: Int = 0
        private set

    val isAvailable: Boolean get() = equalizer != null

    fun attach(context: Context, audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (sessionId == audioSessionId && equalizer != null) return
        release()
        sessionId = audioSessionId
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        runCatching { equalizer = Equalizer(PRIORITY, audioSessionId) }
        runCatching { bassBoost = BassBoost(PRIORITY, audioSessionId) }
        runCatching { virtualizer = Virtualizer(PRIORITY, audioSessionId) }
        restore()
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        sessionId = 0
    }

    // --- Enable ---------------------------------------------------------------------------------
    var isEnabled: Boolean
        get() = equalizer?.enabled ?: false
        set(value) {
            runCatching { equalizer?.enabled = value }
            runCatching { bassBoost?.enabled = value && (bassBoost?.strengthSupported == true) }
            runCatching { virtualizer?.enabled = value && (virtualizer?.strengthSupported == true) }
            prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
        }

    // --- Equalizer bands ------------------------------------------------------------------------
    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0
    val minLevel: Int get() = equalizer?.bandLevelRange?.getOrNull(0)?.toInt() ?: -1500
    val maxLevel: Int get() = equalizer?.bandLevelRange?.getOrNull(1)?.toInt() ?: 1500

    fun centerFreq(band: Int): Int = runCatching { equalizer!!.getCenterFreq(band.toShort()) / 1000 }.getOrDefault(0)
    fun bandLevel(band: Int): Int = runCatching { equalizer!!.getBandLevel(band.toShort()).toInt() }.getOrDefault(0)

    fun setBandLevel(band: Int, millibel: Int) {
        val clamped = millibel.coerceIn(minLevel, maxLevel)
        runCatching { equalizer?.setBandLevel(band.toShort(), clamped.toShort()) }
        persistBands()
    }

    val presetNames: List<String>
        get() = equalizer?.let { eq ->
            runCatching { (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) } }.getOrDefault(emptyList())
        } ?: emptyList()

    fun usePreset(index: Int) {
        runCatching { equalizer?.usePreset(index.toShort()) }
        prefs?.edit()?.putInt(KEY_PRESET, index)?.apply()
        persistBands()
    }

    fun currentBands(): List<Int> = (0 until bandCount).map { bandLevel(it) }

    // --- Bass boost / Virtualizer (0..1000) -----------------------------------------------------
    val bassSupported: Boolean get() = bassBoost?.strengthSupported == true
    var bassStrength: Int
        get() = runCatching { bassBoost?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)
        set(value) {
            if (bassSupported) runCatching { bassBoost?.setStrength(value.coerceIn(0, 1000).toShort()) }
            prefs?.edit()?.putInt(KEY_BASS, value)?.apply()
        }

    val virtualizerSupported: Boolean get() = virtualizer?.strengthSupported == true
    var virtualizerStrength: Int
        get() = runCatching { virtualizer?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0)
        set(value) {
            if (virtualizerSupported) runCatching { virtualizer?.setStrength(value.coerceIn(0, 1000).toShort()) }
            prefs?.edit()?.putInt(KEY_VIRT, value)?.apply()
        }

    // --- Persistence ----------------------------------------------------------------------------
    private fun persistBands() {
        val csv = currentBands().joinToString(",")
        prefs?.edit()?.putString(KEY_BANDS, csv)?.apply()
    }

    private fun restore() {
        val p = prefs ?: return
        val savedBands = p.getString(KEY_BANDS, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
        if (savedBands != null && savedBands.size == bandCount) {
            savedBands.forEachIndexed { band, level ->
                runCatching { equalizer?.setBandLevel(band.toShort(), level.coerceIn(minLevel, maxLevel).toShort()) }
            }
        }
        if (bassSupported) runCatching { bassBoost?.setStrength(p.getInt(KEY_BASS, 0).coerceIn(0, 1000).toShort()) }
        if (virtualizerSupported) runCatching { virtualizer?.setStrength(p.getInt(KEY_VIRT, 0).coerceIn(0, 1000).toShort()) }
        // Apply enabled state last so all effects flip together.
        isEnabled = p.getBoolean(KEY_ENABLED, false)
    }

    private const val PREFS = "audio_effects"
    private const val PRIORITY = 100
    private const val KEY_ENABLED = "enabled"
    private const val KEY_BANDS = "bands"
    private const val KEY_PRESET = "preset"
    private const val KEY_BASS = "bass"
    private const val KEY_VIRT = "virtualizer"
}
