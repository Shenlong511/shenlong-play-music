package com.fran.shenlongplaymusic

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Proveedor de opciones de Google Cast (v1.4).
 *
 * Usa el receptor multimedia por defecto de Google (el que traen los
 * Chromecast, las TV con Google TV y la mayoría de TV boxes): sabe
 * reproducir audio y video por URL sin que la app tenga que instalar
 * nada en la TV. Declarado en el AndroidManifest con el meta-data
 * com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME.
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(
                CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
            )
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
