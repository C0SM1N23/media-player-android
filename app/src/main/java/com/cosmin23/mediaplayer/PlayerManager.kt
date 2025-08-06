package com.cosmin23.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem



class PlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
    }

    fun play(uri: Uri) {
        initPlayer()
        player?.setMediaItem(MediaItem.fromUri(uri))
        player?.prepare()
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun release() {
        player?.release()
        player = null
    }

    fun currentPosition(): Long = player?.currentPosition ?: 0L
    fun duration(): Long = player?.duration ?: 0L
}
