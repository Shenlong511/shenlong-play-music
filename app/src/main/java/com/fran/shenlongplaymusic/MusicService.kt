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
            // (v1.8) El botón Detener de la notificación lleva la
            // generación de reproducción de cuando se construyó: si ya
            // empezó algo nuevo después, el stop es viejo y se ignora.
            if (action == ACTION_STOP) {
                intent.putExtra(EXTRA_STOP_TOKEN, PlayerManager.playbackGeneration.value)
            }
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
            // (v1.8) Se guarda la generación de reproducción actual: si
            // entre pedir el stop y que el servicio lo reciba ya empezó
            // una canción o un video nuevo, ese stop es viejo y no debe
            // parar lo nuevo (el intent de detener es asíncrono).
            intent.putExtra(EXTRA_STOP_TOKEN, PlayerManager.playbackGeneration.value)
            context.startService(intent)
        }

        /** Generación de reproducción que llevaba el intent de detener. */
        const val EXTRA_STOP_TOKEN = "stop_token"
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        PlayerManager.init(this)
        // (v2.0) Si el proceso murió con algo sonando, se recupera lo
        // último guardado (título e imagen) y se vuelve a publicar la
        // notificación ya con la carátula.
        PlayerManager.restoreLastPlayed(this) { updateNotification() }
        val player = PlayerManager.playerOrNull()
        if (player != null) {
            // (v2.5) La actividad es singleTop: al tocar la notificación
            // o el bloqueo se reutiliza la app ya abierta en vez de
            // abrirla otra vez encima. Sin esto, cada toque creaba una
            // segunda MainActivity: había que dar dos veces doble atrás
            // para salir, las dos pantallas se peleaban por la imagen
            // del video y la música se trababa, y al cerrar una se
            // borraba la carátula de la notificación y el bloqueo.
            val activityIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
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
                // (v1.8) Solo si no empezó nada nuevo desde que se pidió
                // el stop: el intent es asíncrono y un stop viejo no debe
                // matar una canción o un video que ya empezó después.
                val token = intent?.getIntExtra(EXTRA_STOP_TOKEN, -1) ?: -1
                if (PlayerManager.stopPlaybackIfNotStartedSince(token)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                // Stop viejo: ya suena algo nuevo; no se para nada y la
                // notificación se actualiza con lo que suena ahora.
                updateNotification()
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
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        // (v2.0) La notificación de reproducción NO debe sonar, pero SÍ
        // se tiene que ver en la cortina: en la 1.8 el canal se puso en
        // IMPORTANCE_LOW (sin sonido) y en el teléfono de Fran la
        // notificación dejó de aparecer en la cortina al bajarla
        // (quedaba escondida entre las silenciosas, y solo se veía la
        // de la grabadora). Con IMPORTANCE_DEFAULT la notificación se
        // ve normal en la cortina y en la barra, y sigue sin sonar
        // porque el canal no tiene sonido (setSound(null, null)) y las
        // actualizaciones no vuelven a avisar (setOnlyAlertOnce). Se
        // borra primero el canal viejo porque Android no cambia la
        // importancia de un canal ya creado.
        manager.deleteNotificationChannel(CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproducción de música",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Controles de Shenlong Play Music"
        channel.setSound(null, null)
        manager.createNotificationChannel(channel)
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
        // (v1.9) Carátula con respaldo: si el elemento actual está a null
        // en una ventana de transición (al cambiar de canción/video o al
        // navegar), se usa el último que sonó, así que la imagen no se
        // pierde nunca mientras suene algo.
        val video = PlayerManager.artworkVideo()
        val song = PlayerManager.artworkSong()
        val isPlaying = PlayerManager.isPlaying.value
        // (v2.5) Igual que la actividad de la sesión: reutilizar la app
        // ya abierta, no abrirla otra vez encima.
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
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
                video?.let { LandscapeArt.bitmapForVideo(it) }
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
