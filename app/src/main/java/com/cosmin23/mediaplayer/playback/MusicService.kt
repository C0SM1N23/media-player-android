package com.cosmin23.mediaplayer.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * The single owner of playback for the whole app.
 *
 * Owns the only [ExoPlayer]; the UI attaches to it through a `MediaController`. Media3 provides —
 * for free — the media notification, lockscreen controls, Bluetooth/headset button handling and
 * Android Auto/Assistant discovery. It also creates a stable audio session id and attaches the
 * [AudioEffects] (equalizer / bass boost / virtualizer) so those keep affecting playback app-wide.
 */
@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Use a deterministic audio session id so the equalizer can attach reliably from the start.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val generated = audioManager.generateAudioSessionId()
        val sessionId = if (generated != AudioManager.ERROR && generated > 0) {
            player.setAudioSessionId(generated)
            generated
        } else {
            player.audioSessionId
        }
        AudioEffects.attach(this, sessionId)

        mediaSession = MediaSession.Builder(this, player).build().also {
            it.setSessionExtras(bundleOf(EXTRA_AUDIO_SESSION_ID to sessionId))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioEffects.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_AUDIO_SESSION_ID = "audio_session_id"
    }
}
