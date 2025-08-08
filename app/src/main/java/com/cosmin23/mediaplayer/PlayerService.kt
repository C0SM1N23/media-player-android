@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.cosmin23.mediaplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media.app.NotificationCompat as MediaAppNotificationCompat

/**
 * Foreground media service simplificat.
 * Asigură-te că în AndroidManifest.xml ai declarat:
 *   <service android:name=".PlayerService"
 *            android:exported="false"
 *            android:foregroundServiceType="mediaPlayback" />
 *
 * Şi ai permisiunea:
 *   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
 *
 * De asemenea adaugă dependenţa androidx.media în build.gradle.kts:
 *   implementation("androidx.media:media:1.6.0")
 */
class PlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "media_player_channel"
        const val NOTIF_ID = 1001

        const val ACTION_PLAY = "com.cosmin23.mediaplayer.action.PLAY"
        const val ACTION_PAUSE = "com.cosmin23.mediaplayer.action.PAUSE"
        const val ACTION_STOP = "com.cosmin23.mediaplayer.action.STOP"

        const val EXTRA_URI = "extra_uri"
    }

    private val binder = LocalBinder()
    private var player: ExoPlayer? = null
    private var currentlyPlaying: Uri? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onCreate() {
        super.onCreate()
        initPlayer()
        createNotificationChannel()
    }

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> {
                    val uriString = intent.getStringExtra(EXTRA_URI)
                    uriString?.let {
                        val uri = Uri.parse(it)
                        playUri(uri)
                    }
                }
                ACTION_PAUSE -> pause()
                ACTION_STOP -> {
                    stop()
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    /** Play given Uri and promote to foreground with a notification. */
    fun playUri(uri: Uri) {
        initPlayer()
        val p = player ?: return
        currentlyPlaying = uri
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.playWhenReady = true
        p.play()

        // POST_NOTIFICATIONS runtime permission required on Android 13+
        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (canPost) {
            // Promote to foreground so system doesn't kill playback
            startForeground(NOTIF_ID, buildNotification(isPlaying = true))
        } else {
            // If no notification permission, still attempt to show a normal notification
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification(isPlaying = true))
        }
    }

    fun pause() {
        player?.pause()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(isPlaying = false))
    }

    fun stop() {
        player?.stop()
        currentlyPlaying = null
        stopForeground(true)
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }

    fun currentPosition(): Long = player?.currentPosition ?: 0L
    fun duration(): Long = player?.duration ?: 0L
    fun audioSessionId(): Int = player?.audioSessionId ?: 0

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val mgrIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, mgrIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val playPauseActionIntent = Intent(this, PlayerService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseActionIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val title = currentlyPlaying?.lastPathSegment ?: "Media playback"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePending
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            // Folosim MediaStyle din androidx.media.app.NotificationCompat (trebuie dependenţa androidx.media)
            .setStyle(
                MediaAppNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Media Player",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Media playback controls"
                nm.createNotificationChannel(channel)
            }
        }
    }
}
