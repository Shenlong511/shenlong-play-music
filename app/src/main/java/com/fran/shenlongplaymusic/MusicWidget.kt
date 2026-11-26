package com.fran.shenlongplaymusic

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Widget de pantalla principal: play/pausa, anterior y siguiente.
 * Los botones mandan la acción al MusicService, que usa el mismo
 * reproductor (PlayerManager) que la app.
 */
class MusicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            val song = PlayerManager.currentSong.value
            val isPlaying = PlayerManager.isPlaying.value
            views.setTextViewText(
                R.id.widget_title,
                song?.title ?: "Shenlong Play Music"
            )
            views.setTextViewText(
                R.id.widget_artist,
                song?.artist ?: "Toca ▶ para escuchar tu música"
            )
            views.setTextViewText(
                R.id.widget_play_pause,
                if (isPlaying) "⏸" else "▶"
            )
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                MusicService.actionIntent(context, MusicService.ACTION_PREVIOUS)
            )
            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                MusicService.actionIntent(context, MusicService.ACTION_PLAY_PAUSE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                MusicService.actionIntent(context, MusicService.ACTION_NEXT)
            )
            // (v2.5) Abrir la app ya abierta, no una segunda copia
            // encima (ver MusicService: singleTop).
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openPending = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
