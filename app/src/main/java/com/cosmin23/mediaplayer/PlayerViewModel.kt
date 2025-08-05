package com.cosmin23.mediaplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = PlayerManager(application)

    fun play() = manager.play(R.raw.music)
    fun stop() = manager.stop()
}
