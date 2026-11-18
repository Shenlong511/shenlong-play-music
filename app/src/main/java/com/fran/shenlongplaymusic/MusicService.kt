package com.fran.shenlongplaymusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession

/**
 * Servicio en primer plano: la música sigue sonando con la pantalla
 * apagada o con la app cerrada. La MediaSession hace que Android muestre
 * los controles (play/pausa, siguiente, anterior, portada y título) en
 * la pantalla de bloqueo y en la notificación.
 */
class MusicService : Service() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.fran.shenlongplaymusic.PLAY_PAUSE"
        const val ACTION_NEXT = "com.fran.shenlongplaymusic.NEXT"
        const val ACTION_PREVIOUS = "com.fran.shenlongplaymusic.PREVIOUS"
        const val ACTION_STOP = "com.fran.shenlongplaymusic.STOP"
        private const val CHANNEL_ID = "shenlong_play_music"
        private const val NOTIFICATION_ID = 1

        // ¿Está el servicio en marcha? Para detenerlo sin arrancarlo
        // de nuevo: stop() no hace nada si ya está parado.
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun actionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicService::class.java).setAction(action)
            return PendingIntent.getService(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /** Detiene por completo la reproducción y quita la notificación. */
        fun stop(context: Context) {
            if (!isRunning) return
            val intent = Intent(context, MusicService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        PlayerManager.init(this)
        val player = PlayerManager.playerOrNull()
        if (player != null) {
            val activityIntent = Intent(this, MainActivity::class.java)
            val sessionActivity = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(this, player)
                .setSessionActivity(sessionActivity)
                .build()
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateNotification()
                }

                override fun onMediaItemTransition(
                    mediaItem: androidx.media3.common.MediaItem?,
                    reason: Int
                ) {
                    updateNotification()
                }
            })
        }
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> PlayerManager.togglePlayPause(this)
            ACTION_NEXT -> PlayerManager.next(this)
            ACTION_PREVIOUS -> PlayerManager.previous(this)
            ACTION_STOP -> {
                // Stop de verdad (v1.4): se para todo, se libera la cola
                // y se quita la notificación. No es pausa.
                PlayerManager.stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de música",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Controles de Shenlong Play Music"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        try {
            manager?.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Sin permiso de notificaciones: la música sigue sonando.
        }
    }

    private fun buildNotification(): Notification {
        // En modo video (v1.3) la notificación muestra el título del video
        // y un paisaje como carátula, con los mismos controles: así el
        // audio del video sigue en segundo plano y se puede pausar o
        // seguir desde la barra o la pantalla de bloqueo.
        val video = PlayerManager.currentVideo.value
        val song = PlayerManager.currentSong.value
        val isPlaying = PlayerManager.isPlaying.value
        val activityIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(video?.title ?: song?.title ?: "Shenlong Play Music")
            .setContentText(
                if (video != null) "Video • Toca para abrir la app"
                else (song?.artist ?: "Toca para abrir la app")
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            // Carátula: el paisaje de la canción actual (o del video).
            // Android no permite animación en vivo en la notificación ni
            // en el bloqueo, así que el paisaje es una imagen fija que
            // cambia con cada canción o video (al pulsar siguiente/anterior
            // se reconstruye la notificación con el paisaje nuevo).
            .setLargeIcon(
                video?.let { LandscapeArt.bitmap((it.id % LandscapeArt.COUNT).toInt()) }
                    ?: song?.let { LandscapeArt.bitmapForSong(it) }
            )
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Estilo de medios: Android trata la notificación como
            // reproductor multimedia y muestra sus controles en la barra
            // de notificaciones y en la pantalla de bloqueo. Sin esto (y
            // sin el permiso POST_NOTIFICATIONS) no salía nada.
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Anterior",
                actionIntent(this, ACTION_PREVIOUS)
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausa" else "Reproducir",
                actionIntent(this, ACTION_PLAY_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Siguiente",
                actionIntent(this, ACTION_NEXT)
            )
            // Detener (v1.4): sale en la notificación expandida; quita
            // la reproducción por completo (no es pausa).
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                actionIntent(this, ACTION_STOP)
            )
            .build()
    }
}
