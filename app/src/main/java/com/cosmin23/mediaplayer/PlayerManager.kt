package com.cosmin23.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build().also {
                it.setAudioAttributes(
                    AudioAttributes.DEFAULT,
                    /* handleAudioFocus= */ true
                )
            }
        }
    }

    fun play(uri: Uri) {
        initPlayer()
        if (uri != currentUri) { // Evită reîncărcarea aceleași melodii
            currentUri = uri
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
        }
        player?.playWhenReady = true
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
        currentUri = null
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun release() {
        player?.release()
        player = null
        currentUri = null
    }

    fun currentPosition(): Long = player?.currentPosition ?: 0L
    fun duration(): Long = player?.duration ?: 0L

    @androidx.media3.common.util.UnstableApi
    fun audioSessionId(): Int {
        return player?.audioSessionId ?: 0
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context).also { instance = it }
            }
        }
    }
}
