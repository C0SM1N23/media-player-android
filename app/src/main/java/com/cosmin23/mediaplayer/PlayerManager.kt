package com.cosmin23.mediaplayer

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

class PlayerManager(private val ctx: Context) {
    private var player: MediaPlayer? = null

    fun play(@RawRes resId: Int) {
        stop()
        player = MediaPlayer.create(ctx, resId)
        player?.start()
    }
    fun stop() {
        player?.release()
        player = null
    }
}
