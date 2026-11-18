package com.fran.shenlongplaymusic

import android.Manifest
import android.graphics.Bitmap
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.content.ContentUris
import android.content.Context
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Rational
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// Modelo
// ---------------------------------------------------------------------------

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val folder: String,
    val uri: Uri,
    val durationMs: Long
) {
    val folderName: String
        get() = File(folder).name.ifBlank { "Sin carpeta" }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------
// Colores: fondo claro + acentos neón
// ---------------------------------------------------------------------------

val NeonBlue = Color(0xFF2196F3)
val NeonPink = Color(0xFFE91E63)
val NeonRed = Color(0xFFF44336)
val NeonGreen = Color(0xFF4CAF50)
val NeonOrange = Color(0xFFFF9800)
val NeonYellow = Color(0xFFFFC107)
val NeonPurple = Color(0xFF9C27B0)
val LightBg = Color(0xFFF7F7FF)
val NeonColors = listOf(NeonBlue, NeonPink, NeonRed, NeonGreen, NeonOrange, NeonYellow, NeonPurple)

// ---------------------------------------------------------------------------
// Ecualizador: presets (niveles en milibelios, -1500 a +1500, 5 bandas)
// ---------------------------------------------------------------------------

val EqPresets: Map<String, List<Short>> = mapOf(
    "Normal" to listOf(0, 0, 0, 0, 0),
    "Rock" to listOf(900, 500, -200, 600, 1000),
    "Pop" to listOf(-300, 400, 800, 500, -200),
    "Jazz" to listOf(500, 300, 0, 400, 700),
    "Clásica" to listOf(400, 200, 0, 300, 800),
    "Graves fuertes" to listOf(1500, 1000, 300, 0, -300),
    // Altavoces/Bocinas: baja graves (el teléfono se distorsiona) y sube
    // medios y agudos para que se escuche claro.
    "Altavoces" to listOf(-1200, -400, 600, 900, 1200)
)
val EqBandNames = listOf("Graves", "Medios-bajos", "Medios", "Medios-altos", "Agudos")

// ---------------------------------------------------------------------------
// Ajustes guardados (DataStore, en el teléfono, sin internet)
// ---------------------------------------------------------------------------

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shenlong_play_music")

class EqSettings(private val context: Context) {
    private val enabledKey = booleanPreferencesKey("eq_enabled")
    private val presetKey = stringPreferencesKey("eq_preset")
    private val bandsKey = stringPreferencesKey("eq_bands")
    private val bassKey = intPreferencesKey("eq_bass")

    val enabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[enabledKey] ?: false }
    val presetFlow: Flow<String> =
        context.dataStore.data.map { it[presetKey] ?: "Normal" }
    val bandsFlow: Flow<List<Short>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[bandsKey] ?: return@map listOf(0, 0, 0, 0, 0)
            raw.split(",").mapNotNull { it.toShortOrNull() }
                .let { if (it.size == 5) it else listOf(0, 0, 0, 0, 0) }
        }
    val bassFlow: Flow<Int> =
        context.dataStore.data.map { it[bassKey] ?: 0 }

    suspend fun save(enabled: Boolean, preset: String, bands: List<Short>, bass: Int) {
        context.dataStore.edit { prefs ->
            prefs[enabledKey] = enabled
            prefs[presetKey] = preset
            prefs[bandsKey] = bands.joinToString(",")
            prefs[bassKey] = bass.coerceIn(0, 1000)
        }
    }
}

// ---------------------------------------------------------------------------
// Listas de reproducción y carpetas ocultas (DataStore, v1.2)
// ---------------------------------------------------------------------------

data class Playlist(val name: String, val songIds: List<Long>)

class PlaylistStore(private val context: Context) {
    private val playlistsKey = stringPreferencesKey("playlists_v1")
    private val hiddenKey = stringSetPreferencesKey("hidden_folders")

    val playlistsFlow: Flow<List<Playlist>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[playlistsKey] ?: return@map emptyList()
            if (raw.isBlank()) return@map emptyList()
            raw.split("\u001E").mapNotNull { entry ->
                val parts = entry.split("\u001F", limit = 2)
                if (parts.isEmpty() || parts[0].isBlank()) null
                else Playlist(
                    parts[0],
                    if (parts.size > 1 && parts[1].isNotBlank())
                        parts[1].split(",").mapNotNull { it.toLongOrNull() }
                    else emptyList()
                )
            }
        }
    val hiddenFoldersFlow: Flow<Set<String>> =
        context.dataStore.data.map { it[hiddenKey] ?: emptySet() }

    suspend fun savePlaylists(list: List<Playlist>) {
        context.dataStore.edit { prefs ->
            prefs[playlistsKey] = list.joinToString("\u001E") { pl ->
                pl.name.replace("\u001E", "").replace("\u001F", "") +
                    "\u001F" + pl.songIds.joinToString(",")
            }
        }
    }

    suspend fun saveHiddenFolders(set: Set<String>) {
        context.dataStore.edit { prefs -> prefs[hiddenKey] = set }
    }
}

// ---------------------------------------------------------------------------
// Color de las tarjetas (DataStore, v1.2): color fijo + transparencia.
// El usuario elige un color fijo de la lista y mueve un deslizador de
// transparencia de 0% (opaco) a 100% (casi transparente). Se guarda el
// nombre del color y la transparencia, y se aplica a todas las tarjetas.
// ---------------------------------------------------------------------------

val CardColorOptions: List<Pair<String, Color>> = listOf(
    "Blanco" to Color.White,
    "Claro" to LightBg,
    "Azul" to NeonBlue,
    "Celeste" to Color(0xFF4FC3F7),
    "Turquesa" to Color(0xFF00BCD4),
    "Verde" to NeonGreen,
    "Lima" to Color(0xFF8BC34A),
    "Amarillo" to NeonYellow,
    "Dorado" to Color(0xFFFFD700),
    "Naranja" to NeonOrange,
    "Rojo" to NeonRed,
    "Rosa" to NeonPink,
    "Fucsia" to Color(0xFFE040FB),
    "Morado" to NeonPurple,
    "Marrón" to Color(0xFF795548),
    "Gris" to Color(0xFF9E9E9E),
    "Negro" to Color.Black
)

class CardColorSettings(private val context: Context) {
    private val colorKey = stringPreferencesKey("card_color_name")
    private val transparencyKey = floatPreferencesKey("card_transparency")

    val colorNameFlow: Flow<String> =
        context.dataStore.data.map { it[colorKey] ?: "Blanco" }
    val transparencyFlow: Flow<Float> =
        context.dataStore.data.map { (it[transparencyKey] ?: 0f).coerceIn(0f, 1f) }

    suspend fun save(colorName: String, transparency: Float) {
        context.dataStore.edit { prefs ->
            prefs[colorKey] = colorName
            prefs[transparencyKey] = transparency.coerceIn(0f, 1f)
        }
    }
}

// ---------------------------------------------------------------------------
// Color de fondo de la app (DataStore, v1.3): igual que el de tarjetas,
// color fijo + transparencia, aplicado al fondo general de la app.
// Por defecto "Claro" (= LightBg), el fondo que ya tenía la app.
// ---------------------------------------------------------------------------

class BackgroundColorSettings(private val context: Context) {
    private val colorKey = stringPreferencesKey("bg_color_name")
    private val transparencyKey = floatPreferencesKey("bg_transparency")

    val colorNameFlow: Flow<String> =
        context.dataStore.data.map { it[colorKey] ?: "Claro" }
    val transparencyFlow: Flow<Float> =
        context.dataStore.data.map { (it[transparencyKey] ?: 0f).coerceIn(0f, 1f) }

    suspend fun save(colorName: String, transparency: Float) {
        context.dataStore.edit { prefs ->
            prefs[colorKey] = colorName
            prefs[transparencyKey] = transparency.coerceIn(0f, 1f)
        }
    }
}

// ---------------------------------------------------------------------------
// Giro de pantalla (DataStore, v1.3): si está apagado, la app queda fija
// en vertical y el botón de giro no hace nada.
// ---------------------------------------------------------------------------

class RotationSettings(private val context: Context) {
    private val allowedKey = booleanPreferencesKey("rotation_allowed")

    val allowedFlow: Flow<Boolean> =
        context.dataStore.data.map { it[allowedKey] ?: true }

    suspend fun save(allowed: Boolean) {
        context.dataStore.edit { prefs -> prefs[allowedKey] = allowed }
    }
}

// ---------------------------------------------------------------------------
// Estilo de visualización (DataStore, v1.2): 4 estilos sobre los mismos
// datos FFT del VisualizerManager. Se guarda el nombre elegido y se
// aplica tanto en la tarjeta "Sonando ahora" como a pantalla completa.
// ---------------------------------------------------------------------------

val VisualizerStyleOptions: List<Pair<String, String>> = listOf(
    "Barras neón" to "28 barras de colores neón, como el reproductor de Windows.",
    "Onda" to "Una línea de onda continua que sigue la música.",
    "Círculo" to "Barras en círculo alrededor del centro.",
    "Espejo" to "Barras espejo con puntos, arriba y abajo del centro."
)

class VisualizerSettings(private val context: Context) {
    private val styleKey = stringPreferencesKey("visualizer_style")

    val styleFlow: Flow<String> =
        context.dataStore.data.map { it[styleKey] ?: "Barras neón" }

    suspend fun save(style: String) {
        context.dataStore.edit { prefs -> prefs[styleKey] = style }
    }
}

// ---------------------------------------------------------------------------
// Ecualizador real (android.media.audiofx) sobre el audioSessionId de ExoPlayer
// ---------------------------------------------------------------------------

object EqManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    var bandCount: Int = 5
        private set
    var minLevel: Short = -1500
        private set
    var maxLevel: Short = 1500
        private set
    // Últimos ajustes pedidos: se reaplican al conectar el ecualizador a
    // un audioSessionId nuevo (al arrancar el reproductor), porque el
    // Equalizer nuevo nace apagado y con niveles en 0.
    private var lastEnabled: Boolean = false
    private var lastLevels: List<Short> = listOf(0, 0, 0, 0, 0)
    private var lastBass: Int = 0

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false
                bandCount = numberOfBands.toInt()
                val range = bandLevelRange
                if (range.size >= 2) {
                    minLevel = range[0]
                    maxLevel = range[1]
                }
            }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
            applyLevels(lastLevels)
            setBass(lastBass)
            setEnabled(lastEnabled)
        } catch (e: Exception) {
            release()
        }
    }

    fun release() {
        try { equalizer?.release() } catch (e: Exception) { }
        try { bassBoost?.release() } catch (e: Exception) { }
        equalizer = null
        bassBoost = null
    }

    /** Aplica niveles (5 valores); si el equipo tiene otro número de bandas,
     *  se reparten proporcionalmente. */
    fun applyLevels(levels: List<Short>) {
        lastLevels = levels
        val eq = equalizer ?: return
        try {
            val n = eq.numberOfBands.toInt()
            for (i in 0 until n) {
                val src = levels[(i * levels.size / n).coerceIn(0, levels.size - 1)]
                eq.setBandLevel(i.toShort(), src.coerceIn(minLevel, maxLevel))
            }
        } catch (e: Exception) { }
    }

    fun setEnabled(enabled: Boolean) {
        lastEnabled = enabled
        try { equalizer?.enabled = enabled } catch (e: Exception) { }
        try { bassBoost?.enabled = enabled } catch (e: Exception) { }
    }

    /** Fuerza del refuerzo de bajos: 0 a 1000. */
    fun setBass(strength: Int) {
        lastBass = strength.coerceIn(0, 1000)
        try { bassBoost?.setStrength(lastBass.toShort()) } catch (e: Exception) { }
    }
}

// ---------------------------------------------------------------------------
// Visualizador de audio estilo Windows Media Player: barras verticales
// de colores neón que suben y bajan al ritmo de la música.
// Usa android.media.audiofx.Visualizer sobre el MISMO audioSessionId de
// ExoPlayer que ya usa el ecualizador, así que no interfiere con él.
// No necesita el permiso RECORD_AUDIO: el Visualizer lee la salida de
// audio de la propia app a través de su audioSessionId.
// Si el Visualizer no está disponible en el dispositivo (o devuelve un
// error), no rompe la app: se activa el modo de respaldo, con barras
// animadas suaves, y la música sigue sonando normal.
// ---------------------------------------------------------------------------

object VisualizerManager {
    const val BAR_COUNT = 28
    private const val MIN_LEVEL = 0.06f

    private var visualizer: Visualizer? = null
    private var playing: Boolean = false
    private val smoothed = FloatArray(BAR_COUNT) { MIN_LEVEL }

    private val _levels = MutableStateFlow(List(BAR_COUNT) { MIN_LEVEL })
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()

    private val _fallback = MutableStateFlow(false)
    /** true cuando el Visualizer real no está disponible y hay que animar. */
    val fallback: StateFlow<Boolean> = _fallback.asStateFlow()

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        try {
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = range[1].coerceIn(range[0], 1024)
            val viz = Visualizer(audioSessionId)
            viz.captureSize = captureSize
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Solo se usa FFT; la forma de onda se ignora.
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null || !playing) return
                        updateLevelsFromFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            viz.enabled = playing
            visualizer = viz
            _fallback.value = false
        } catch (e: Exception) {
            // Visualizer no disponible en este dispositivo: respaldo animado.
            release()
            _fallback.value = true
        }
    }

    /** Pausa o reanuda la captura: en pausa las barras caen al mínimo. */
    fun setPlaying(isPlaying: Boolean) {
        playing = isPlaying
        try { visualizer?.enabled = isPlaying } catch (e: Exception) { }
        if (!isPlaying) {
            for (i in smoothed.indices) smoothed[i] = MIN_LEVEL
            _levels.value = List(BAR_COUNT) { MIN_LEVEL }
        }
    }

    fun release() {
        try { visualizer?.enabled = false } catch (e: Exception) { }
        try { visualizer?.release() } catch (e: Exception) { }
        visualizer = null
        for (i in smoothed.indices) smoothed[i] = MIN_LEVEL
        _levels.value = List(BAR_COUNT) { MIN_LEVEL }
    }

    /**
     * Convierte la FFT en 28 alturas de barra (0.06 a 1.0).
     * Los bins se reparten en escala logarítmica, como en los
     * visualizadores clásicos: más detalle en los graves.
     */
    private fun updateLevelsFromFft(fft: ByteArray) {
        val bins = fft.size / 2
        if (bins < 2) return
        val next = FloatArray(BAR_COUNT)
        for (bar in 0 until BAR_COUNT) {
            val start = (bins * Math.pow(
                bar.toDouble() / BAR_COUNT, 1.6
            )).toInt().coerceIn(0, bins - 1)
            val end = (bins * Math.pow(
                (bar + 1).toDouble() / BAR_COUNT, 1.6
            )).toInt().coerceIn(start + 1, bins)
            var maxMag = 0f
            var i = start
            while (i < end) {
                val re = fft[2 * i].toInt()
                val im = fft[2 * i + 1].toInt()
                val mag = sqrt((re * re + im * im).toFloat())
                if (mag > maxMag) maxMag = mag
                i++
            }
            // Escala logarítmica: mag típica 0..~130 -> nivel 0..1.
            val target = (ln(1f + maxMag / 12f) / ln(1f + 130f / 12f))
                .coerceIn(MIN_LEVEL, 1f)
            // Suavizado: sube rápido, baja despacio (efecto de caída).
            smoothed[bar] = if (target > smoothed[bar]) target
            else (smoothed[bar] * 0.82f + target * 0.18f).coerceAtLeast(MIN_LEVEL)
            next[bar] = smoothed[bar]
        }
        _levels.value = next.toList()
    }
}

// ---------------------------------------------------------------------------
// Reproductor (singleton compartido por la UI, el servicio y el widget)
// ---------------------------------------------------------------------------

object PlayerManager {
    private var player: ExoPlayer? = null
    private var scope: CoroutineScope? = null
    private var attachedSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    /** 0 = sin repetir, 1 = repetir lista, 2 = repetir una. */
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    var playlist: List<Song> = emptyList()
        private set
    var currentIndex: Int = -1
        private set

    // Video en curso (v1.3): mismo ExoPlayer y mismo servicio que la
    // música, así que el audio sigue en segundo plano. Cuando no es
    // null, el reproductor está en modo video.
    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()
    var videoPlaylist: List<VideoItem> = emptyList()
        private set
    var currentVideoIndex: Int = -1
        private set

    fun pause() {
        player?.pause()
    }

    /**
     * STOP de verdad (v1.4): detiene por completo la reproducción, tanto
     * de música como de video. No es pausa: se para el reproductor, se
     * libera la cola (lista y video) y se borra lo que estaba sonando,
     * así que la notificación y la tarjeta "Sonando ahora" desaparecen.
     * El servicio se quita con MusicService.stop(context).
     */
    fun stopPlayback() {
        val p = player
        p?.stop()
        p?.clearMediaItems()
        p?.setVideoSurfaceView(null)
        lastVideoSurface = null
        playlist = emptyList()
        currentIndex = -1
        videoPlaylist = emptyList()
        currentVideoIndex = -1
        _currentSong.value = null
        _currentVideo.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        _isPlaying.value = false
        VisualizerManager.setPlaying(false)
    }

    /** Superficie donde se dibuja el video; null = solo audio (segundo plano). */
    private var lastVideoSurface: SurfaceView? = null

    fun setVideoSurface(view: SurfaceView?) {
        lastVideoSurface = view
        player?.setVideoSurfaceView(view)
    }

    /**
     * Reengancha la imagen del video (v1.4): al volver de la ventanita
     * flotante (PiP) a la app, Android puede haber soltado la superficie
     * del SurfaceView; sin volver a engancharla, el video sigue solo con
     * el audio. Se llama al salir del modo PiP y cuando se crea una
     * superficie nueva (SurfaceHolder.Callback en las pantallas de video).
     */
    fun reattachVideoSurface() {
        val view = lastVideoSurface ?: return
        player?.setVideoSurfaceView(null)
        player?.setVideoSurfaceView(view)
    }

    fun init(context: Context) {
        if (player != null) return
        val appContext = context.applicationContext
        player = ExoPlayer.Builder(appContext).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    _durationMs.value = duration
                    VisualizerManager.setPlaying(isPlaying)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _durationMs.value = duration
                    if (state == Player.STATE_ENDED) {
                        if (_repeatMode.value == 2) {
                            seekTo(0)
                            play()
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val rawId = mediaItem?.mediaId
                    if (rawId != null && rawId.startsWith("v")) {
                        // Video: mediaId "v0", "v1"... (índice en videoPlaylist).
                        val idx = rawId.removePrefix("v").toIntOrNull()
                        if (idx != null && idx in videoPlaylist.indices) {
                            currentVideoIndex = idx
                            _currentVideo.value = videoPlaylist[idx]
                        }
                    } else {
                        val idx = rawId?.toIntOrNull()
                        if (idx != null && idx in playlist.indices) {
                            currentIndex = idx
                            _currentSong.value = playlist[idx]
                        }
                    }
                    _durationMs.value = duration
                }
            })
        }
        scope = CoroutineScope(Dispatchers.Main).also { s ->
            s.launch {
                while (true) {
                    val p = player
                    if (p != null) {
                        _positionMs.value = p.currentPosition
                        _durationMs.value = if (p.duration > 0) p.duration else _durationMs.value
                        if (p.audioSessionId != C.AUDIO_SESSION_ID_UNSET &&
                            p.audioSessionId != attachedSessionId
                        ) {
                            attachedSessionId = p.audioSessionId
                            EqManager.attach(p.audioSessionId)
                            VisualizerManager.attach(p.audioSessionId)
                        }
                    }
                    delay(500)
                }
            }
        }
    }

    fun playerOrNull(): ExoPlayer? = player

    fun loadPlaylist(songs: List<Song>, startIndex: Int, context: Context) {
        init(context)
        val p = player ?: return
        // Al sonar una canción se sale del modo video (y se quita la
        // imagen). Si el video estaba en la ventanita PiP, la app sale
        // de la ventanita y vuelve a pantalla completa.
        if (Build.VERSION.SDK_INT >= 26 && context is Activity &&
            context.isInPictureInPictureMode
        ) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
        _currentVideo.value = null
        videoPlaylist = emptyList()
        currentVideoIndex = -1
        p.setVideoSurfaceView(null)
        playlist = songs
        currentIndex = startIndex.coerceIn(0, songs.size - 1)
        val items = songs.mapIndexed { i, song ->
            // Carátula: un paisaje distinto por canción (LandscapeArt).
            // La MediaSession la muestra en la pantalla de bloqueo; la
            // notificación usa el mismo paisaje como icono grande.
            // Solo hay 20 paisajes y se guardan en caché, así que no se
            // vuelve a dibujar nada aunque la lista tenga muchas canciones.
            MediaItem.Builder().setUri(song.uri).setMediaId(i.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkData(
                            LandscapeArt.artworkBytesForSong(song),
                            MediaMetadata.PICTURE_TYPE_FRONT_COVER
                        )
                        .build()
                )
                .build()
        }
        p.setMediaItems(items, currentIndex, 0)
        p.prepare()
        p.play()
        _currentSong.value = songs[currentIndex]
        MusicService.start(context)
    }

    fun playSong(song: Song, allSongs: List<Song>, context: Context) {
        val idx = allSongs.indexOf(song)
        if (idx >= 0) loadPlaylist(allSongs, idx, context)
    }

    // ---- Video (v1.3): mismo reproductor y servicio que la música ----
    // Al poner un video, la canción que sonaba se detiene sola porque es
    // el mismo ExoPlayer; el servicio en primer plano mantiene el audio
    // aunque se apague la pantalla o se minimice la app. La imagen solo
    // se ve mientras la pantalla de video está abierta (superficie);
    // al cerrarla se quita la superficie pero el audio sigue.
    fun playVideo(context: Context, video: VideoItem, allVideos: List<VideoItem>) {
        init(context)
        val p = player ?: return
        // Al poner un video, la canción que sonaba se DETIENE por
        // completo (v1.4): se borra la lista de música y lo que sonaba,
        // no solo se pausa.
        _currentSong.value = null
        playlist = emptyList()
        currentIndex = -1
        videoPlaylist = allVideos
        currentVideoIndex = allVideos.indexOf(video).coerceAtLeast(0)
        val items = allVideos.mapIndexed { i, item ->
            MediaItem.Builder().setUri(item.uri).setMediaId("v$i")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist("Video")
                        .build()
                )
                .build()
        }
        p.setMediaItems(items, currentVideoIndex, 0)
        p.prepare()
        p.play()
        _currentVideo.value = allVideos[currentVideoIndex]
        MusicService.start(context)
    }

    fun nextVideo(context: Context) {
        val p = player ?: return
        if (videoPlaylist.isEmpty()) return
        if (currentVideoIndex < videoPlaylist.size - 1) {
            p.seekToNextMediaItem()
            p.play()
            MusicService.start(context)
        }
    }

    fun previousVideo(context: Context) {
        val p = player ?: return
        if (videoPlaylist.isEmpty()) return
        if (p.currentPosition > 3000) {
            p.seekTo(0)
        } else {
            p.seekToPreviousMediaItem()
        }
        p.play()
        MusicService.start(context)
    }

    fun togglePlayPause(context: Context) {
        val p = player ?: return
        if (p.isPlaying) p.pause() else {
            p.play()
            MusicService.start(context)
        }
    }

    fun next(context: Context) {
        val p = player ?: return
        if (_currentVideo.value != null) {
            nextVideo(context)
            return
        }
        if (playlist.isEmpty()) return
        if (_shuffle.value) {
            val nextIdx = (playlist.indices - currentIndex).randomOrNull() ?: return
            p.seekTo(nextIdx, 0)
        } else {
            p.seekToNextMediaItem()
        }
        p.play()
        MusicService.start(context)
    }

    fun previous(context: Context) {
        val p = player ?: return
        if (_currentVideo.value != null) {
            previousVideo(context)
            return
        }
        if (playlist.isEmpty()) return
        if (p.currentPosition > 3000) {
            p.seekTo(0)
        } else {
            p.seekToPreviousMediaItem()
        }
        p.play()
        MusicService.start(context)
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms.coerceIn(0, (player?.duration ?: 0).coerceAtLeast(0)))
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        player?.shuffleModeEnabled = _shuffle.value
    }

    fun cycleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        player?.repeatMode = when (_repeatMode.value) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun repeatLabel(): String = when (_repeatMode.value) {
        1 -> "Repetir lista"
        2 -> "Repetir una"
        else -> "Sin repetir"
    }
}

// ---------------------------------------------------------------------------
// Lectura de canciones del teléfono (MediaStore, música local)
// ---------------------------------------------------------------------------

fun loadSongs(context: Context): List<Song> {
    val out = mutableListOf<Song>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
        "${MediaStore.Audio.Media.DURATION} > 0"
    val sort = "${MediaStore.Audio.Media.TITLE} ASC"
    try {
        context.contentResolver.query(uri, projection, selection, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: continue
                val folder = File(path).parent ?: ""
                out.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Sin título",
                        artist = cursor.getString(artistCol) ?: "Artista desconocido",
                        album = cursor.getString(albumCol) ?: "",
                        folder = folder,
                        uri = ContentUris.withAppendedId(uri, id),
                        durationMs = cursor.getLong(durCol)
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Sin permiso o sin música: se devuelve la lista vacía y la UI lo explica.
    }
    return out
}

// ---------------------------------------------------------------------------
// Videos del teléfono (MediaStore.Video, v1.3): MP4, MKV, WebM, 3GP y
// demás formatos comunes los reproduce ExoPlayer sin nada extra.
// Se usa el MISMO ExoPlayer y el MISMO servicio en primer plano que la
// música, así que el audio del video sigue sonando en segundo plano
// (al minimizar la app o apagar la pantalla), con los controles en la
// notificación, como YouTube Premium. Al volver a la app se retoma la
// imagen con "Ver video".
// ---------------------------------------------------------------------------

data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val uri: Uri,
    val width: Int,
    val height: Int,
    // Carpeta del video (v1.4): los videos se agrupan por carpeta como
    // la música y se pueden ocultar/mostrar con las mismas carpetas
    // ocultas.
    val folder: String
) {
    val folderName: String
        get() = File(folder).name.ifBlank { "Sin carpeta" }
}

fun loadVideos(context: Context): List<VideoItem> {
    val out = mutableListOf<VideoItem>()
    val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.DATA
    )
    val selection = "${MediaStore.Video.Media.DURATION} > 0"
    val sort = "${MediaStore.Video.Media.TITLE} ASC"
    try {
        context.contentResolver.query(uri, projection, selection, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                out.add(
                    VideoItem(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol) ?: "Sin título",
                        durationMs = cursor.getLong(durCol),
                        uri = ContentUris.withAppendedId(uri, cursor.getLong(idCol)),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        folder = File(cursor.getString(dataCol) ?: "").parent ?: ""
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Sin permiso o sin videos: se devuelve la lista vacía y la UI lo explica.
    }
    return out
}

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) ==
        PackageManager.PERMISSION_GRANTED

// Permiso de notificaciones (Android 13+): sin él, en Android 13+ el
// sistema bloquea la notificación del servicio y no sale el reproductor
// ni en la barra de notificaciones ni en la pantalla de bloqueo.
fun notificationPermission(): String? =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null

fun hasNotificationPermission(context: Context): Boolean {
    val permission = notificationPermission() ?: return true
    return ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
}

// Permiso de video (Android 13+): READ_MEDIA_VIDEO es un permiso aparte
// de READ_MEDIA_AUDIO. En Android 12 o menos, el permiso de
// almacenamiento (READ_EXTERNAL_STORAGE) ya cubre los videos.
fun videoPermission(): String? =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else null

fun hasVideoPermission(context: Context): Boolean {
    val permission = videoPermission() ?: return hasAudioPermission(context)
    return ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
}

/** Permisos que se piden juntos al abrir la app: audio + video + notificaciones. */
fun permissionsToRequest(): Array<String> {
    val list = mutableListOf(audioPermission())
    videoPermission()?.let { list.add(it) }
    notificationPermission()?.let { list.add(it) }
    return list.toTypedArray()
}

// ---------------------------------------------------------------------------
// Actividad principal
// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    companion object {
        // Puente entre la Activity (onUserLeaveHint /
        // onPictureInPictureModeChanged) y Compose: si la pantalla de
        // video está abierta y si la app está en la ventanita PiP.
        val isInPipFlow = MutableStateFlow(false)
        var videoScreenOpen: Boolean = false
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerManager.init(this)
        // Transmisión Cast (v1.4): escucha si se conecta una TV.
        CastManager.init(this)
        setContent { ShenlongPlayMusicApp() }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Botón de inicio con un video reproduciéndose a pantalla
        // completa: la app entra en modo Picture-in-Picture y el video
        // sigue viéndose en una ventanita flotante por encima de otras
        // apps, que Android deja mover y cambiar de tamaño arrastrando
        // sus esquinas. Al pulsar la ventanita se vuelve a pantalla
        // completa (acción por defecto de Android).
        if (videoScreenOpen && PlayerManager.isPlaying.value) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipFlow.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) updatePipParams()
        else {
            // Al volver de la ventanita a la app (v1.4): se vuelve a
            // enganchar la imagen del video, que si no solo sonaría el
            // audio. La pantalla de video también lo reengancha al
            // recomponerse (LaunchedEffect en isInPip).
            PlayerManager.reattachVideoSurface()
        }
    }

    /** Parámetros PiP: ventana 16:9 con el botón play/pausa. */
    private fun pipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < 26) return null
        val isPlaying = PlayerManager.isPlaying.value
        val action = RemoteAction(
            Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "Pausa" else "Reproducir",
            if (isPlaying) "Pausar el video" else "Reproducir el video",
            MusicService.actionIntent(this, MusicService.ACTION_PLAY_PAUSE)
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .build()
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT < 26) return
        if (isInPictureInPictureMode) return
        try {
            pipParams()?.let { enterPictureInPictureMode(it) }
        } catch (e: Exception) {
            // Si el teléfono no permite PiP (por ejemplo, está
            // desactivado en ajustes), el video sigue en segundo plano
            // solo con el audio, como ya hacía la v1.3.
        }
    }

    /** Actualiza el botón play/pausa de la ventanita al cambiar el estado. */
    fun updatePipParams() {
        if (Build.VERSION.SDK_INT < 26) return
        if (!isInPictureInPictureMode) return
        try {
            pipParams()?.let { setPictureInPictureParams(it) }
        } catch (e: Exception) {
            // Sin PiP activo no hay nada que actualizar.
        }
    }

    /** Saca a la app de la ventanita PiP y la vuelve a pantalla completa. */
    fun exitPip() {
        if (Build.VERSION.SDK_INT < 26) return
        if (!isInPictureInPictureMode) return
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }
}

@Composable
fun ShenlongPlayMusicApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val eqSettings = remember { EqSettings(context) }

    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    var hasNotifPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showEq by remember { mutableStateOf(false) }
    var showPlaylists by remember { mutableStateOf(false) }
    // Videos (v1.3): lista propia y video cuya pantalla está abierta.
    // playingVideo == null pero currentVideo != null = video sonando en
    // segundo plano (solo audio); se retoma con "Ver video".
    var showVideos by remember { mutableStateOf(false) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var hasVideoPermission by remember { mutableStateOf(hasVideoPermission(context)) }
    var playingVideo by remember { mutableStateOf<VideoItem?>(null) }
    // ¿Estaba abierta la pantalla de video al entrar en la ventanita
    // PiP? Solo en ese caso se vuelve a abrir al salir de PiP (v1.4):
    // si el usuario cerró el video a propósito, no se reabre.
    var pipVideoScreenOpen by remember { mutableStateOf(false) }
    // Carpeta de videos elegida (v1.4): null = lista de carpetas.
    var selectedVideoFolder by remember { mutableStateOf<String?>(null) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showHiddenFolders by remember { mutableStateOf(false) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    val playlistStore = remember { PlaylistStore(context) }
    val cardColorSettings = remember { CardColorSettings(context) }
    var cardColorName by remember { mutableStateOf("Blanco") }
    var cardTransparency by remember { mutableStateOf(0f) }
    var showCardColorDialog by remember { mutableStateOf(false) }
    // Color de fondo (v1.3) y giro de pantalla (v1.3).
    val bgColorSettings = remember { BackgroundColorSettings(context) }
    var bgColorName by remember { mutableStateOf("Claro") }
    var bgTransparency by remember { mutableStateOf(0f) }
    var showBgColorDialog by remember { mutableStateOf(false) }
    val rotationSettings = remember { RotationSettings(context) }
    var rotationAllowed by remember { mutableStateOf(true) }
    val visualizerSettings = remember { VisualizerSettings(context) }
    var vizStyle by remember { mutableStateOf("Barras neón") }
    var showVizStyleDialog by remember { mutableStateOf(false) }
    // Orientación: para la visualización a pantalla completa.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity
    var pendingOrientation by remember { mutableStateOf<Int?>(null) }
    // Cuando se llega a la orientación pedida con el botón de giro, se
    // libera el bloqueo para que el sensor del teléfono vuelva a mandar.
    LaunchedEffect(configuration.orientation) {
        if (pendingOrientation != null && pendingOrientation == configuration.orientation) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            pendingOrientation = null
        }
    }
    fun rotateToLandscape() {
        if (!rotationAllowed) return
        pendingOrientation = Configuration.ORIENTATION_LANDSCAPE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    fun rotateToPortrait() {
        pendingOrientation = Configuration.ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    // Giro apagado (v1.3): la app queda fija en vertical. Al volver a
    // activarlo se libera el bloqueo y el sensor vuelve a mandar.
    LaunchedEffect(rotationAllowed) {
        if (!rotationAllowed) {
            pendingOrientation = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    val playlists by playlistStore.playlistsFlow.collectAsState(initial = emptyList())
    val hiddenFolders by playlistStore.hiddenFoldersFlow.collectAsState(initial = emptySet())

    var eqEnabled by remember { mutableStateOf(false) }
    var eqPreset by remember { mutableStateOf("Normal") }
    var eqBands by remember { mutableStateOf(listOf<Short>(0, 0, 0, 0, 0)) }
    var eqBass by remember { mutableStateOf(0) }

    val currentSong by PlayerManager.currentSong.collectAsState()
    val currentVideo by PlayerManager.currentVideo.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    // Picture-in-Picture (v1.3): si estamos en la ventanita flotante,
    // la pantalla de video esconde sus controles (la ventanita lleva
    // solo su botón play/pausa) y al cambiar play/pausa se actualiza
    // el botón de la ventanita.
    val isInPip by MainActivity.isInPipFlow.collectAsState()
    LaunchedEffect(playingVideo) {
        MainActivity.videoScreenOpen = playingVideo != null
    }
    LaunchedEffect(isPlaying, isInPip) {
        if (isInPip) (activity as? MainActivity)?.updatePipParams()
    }
    // Al volver de la ventanita flotante (v1.4): si había un video
    // sonando y la pantalla de video se había perdido, se vuelve a
    // abrir para que se VEA el video y no solo se oiga el audio.
    // También se reengancha la superficie (ver reattachVideoSurface).
    LaunchedEffect(isInPip) {
        if (isInPip) {
            pipVideoScreenOpen = playingVideo != null
        } else if (pipVideoScreenOpen && currentVideo != null && playingVideo == null) {
            playingVideo = currentVideo
            pipVideoScreenOpen = false
        }
    }
    val positionMs by PlayerManager.positionMs.collectAsState()
    val durationMs by PlayerManager.durationMs.collectAsState()
    val shuffle by PlayerManager.shuffle.collectAsState()
    val repeatMode by PlayerManager.repeatMode.collectAsState()

    // Al salir de la app se libera el Visualizer (sin fugas) junto con
    // el resto del reproductor.
    DisposableEffect(Unit) {
        onDispose { VisualizerManager.release() }
    }

    // Cargar ajustes del ecualizador guardados y aplicarlos.
    LaunchedEffect(Unit) {
        eqEnabled = eqSettings.enabledFlow.first()
        eqPreset = eqSettings.presetFlow.first()
        eqBands = eqSettings.bandsFlow.first()
        eqBass = eqSettings.bassFlow.first()
        cardColorName = cardColorSettings.colorNameFlow.first()
        cardTransparency = cardColorSettings.transparencyFlow.first()
        bgColorName = bgColorSettings.colorNameFlow.first()
        bgTransparency = bgColorSettings.transparencyFlow.first()
        rotationAllowed = rotationSettings.allowedFlow.first()
        vizStyle = visualizerSettings.styleFlow.first()
        EqManager.setEnabled(eqEnabled)
        if (eqEnabled) {
            EqManager.applyLevels(eqBands)
            EqManager.setBass(eqBass)
        }
    }

    fun saveEq() {
        scope.launch { eqSettings.save(eqEnabled, eqPreset, eqBands, eqBass) }
    }

    fun savePlaylists(list: List<Playlist>) {
        scope.launch { playlistStore.savePlaylists(list) }
    }

    fun saveHiddenFolders(set: Set<String>) {
        scope.launch { playlistStore.saveHiddenFolders(set) }
    }

    fun saveCardColor() {
        scope.launch { cardColorSettings.save(cardColorName, cardTransparency) }
    }

    fun saveBgColor() {
        scope.launch { bgColorSettings.save(bgColorName, bgTransparency) }
    }

    fun saveRotation() {
        scope.launch { rotationSettings.save(rotationAllowed) }
    }

    fun saveVizStyle() {
        scope.launch { visualizerSettings.save(vizStyle) }
    }

    // Color de tarjetas elegido + transparencia: 0% = opaco, 100% =
    // casi transparente (alpha 0). Se aplica a todas las tarjetas.
    val cardColor = CardColorOptions.firstOrNull { it.first == cardColorName }?.second ?: Color.White
    val cardBg = cardColor.copy(alpha = (1f - cardTransparency).coerceIn(0f, 1f))
    // Color de fondo de la app (v1.3), igual: color fijo + transparencia.
    val bgColor = CardColorOptions.firstOrNull { it.first == bgColorName }?.second ?: LightBg
    val appBg = bgColor.copy(alpha = (1f - bgTransparency).coerceIn(0f, 1f))

    fun refreshSongs() {
        songs = loadSongs(context)
    }

    fun refreshVideos() {
        videos = loadVideos(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[audioPermission()] == true
        hasVideoPermission = videoPermission()?.let { result[it] == true }
            ?: (result[audioPermission()] == true)
        hasNotifPermission = notificationPermission()?.let { result[it] == true } ?: true
        if (hasPermission) refreshSongs()
        if (hasVideoPermission) refreshVideos()
    }

    // Al abrir la app se piden juntos audio + video + notificaciones
    // (Android 13+). Sin el de notificaciones la música sigue sonando,
    // pero Android no muestra los controles en la barra ni en la pantalla
    // de bloqueo.
    LaunchedEffect(Unit) {
        if (!hasPermission || !hasVideoPermission || !hasNotifPermission) {
            permissionLauncher.launch(permissionsToRequest())
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) refreshSongs()
    }

    LaunchedEffect(hasVideoPermission) {
        if (hasVideoPermission) refreshVideos()
    }

    // Lista visible: búsqueda + filtro por carpeta.
    val visibleSongs = remember(songs, query, selectedFolder, hiddenFolders) {
        songs.filter { song ->
            val matchesQuery = query.isBlank() ||
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
            val matchesFolder = selectedFolder == null || song.folder == selectedFolder
            val notHidden = song.folder !in hiddenFolders
            matchesQuery && matchesFolder && notHidden
        }
    }
    // Canciones de la lista elegida (por orden en que se agregaron).
    val selectedPlaylistObj = playlists.firstOrNull { it.name == selectedPlaylist }
    val playlistSongs = remember(songs, selectedPlaylistObj) {
        selectedPlaylistObj?.songIds?.mapNotNull { id ->
            songs.firstOrNull { it.id == id }
        } ?: emptyList()
    }
    val folders = remember(songs, hiddenFolders) {
        songs.filter { it.folder !in hiddenFolders }
            .groupBy { it.folder }.map { (folder, list) -> folder to list.size }
            .sortedBy { File(it.first).name.lowercase() }
    }
    // Carpetas de videos (v1.4): igual que las de música, con las
    // mismas carpetas ocultas (ocultar una carpeta la quita de música
    // y de videos a la vez).
    val videoFolders = remember(videos, hiddenFolders) {
        videos.filter { it.folder !in hiddenFolders }
            .groupBy { it.folder }.map { (folder, list) -> folder to list.size }
            .sortedBy { File(it.first).name.lowercase() }
    }
    val selectedVideoVideos = remember(videos, selectedVideoFolder, hiddenFolders) {
        videos.filter { it.folder == selectedVideoFolder && it.folder !in hiddenFolders }
    }

    // Cerrar la pantalla de video (v1.3): se quita la imagen pero el
    // audio sigue en segundo plano; se vuelve a la lista y el video se
    // retoma con "Ver video". No se reabre al salir de PiP porque se
    // marca pipVideoScreenOpen = false antes de salir de la ventanita.
    fun closeVideoScreen() {
        pipVideoScreenOpen = false
        (activity as? MainActivity)?.exitPip()
        playingVideo = null
        showVideos = false; showFolders = false; showEq = false
        showPlaylists = false
        selectedFolder = null; selectedPlaylist = null
        selectedVideoFolder = null
    }

    // Detener el video por completo (v1.4) y volver a la lista de videos.
    fun stopVideoScreen() {
        pipVideoScreenOpen = false
        (activity as? MainActivity)?.exitPip()
        PlayerManager.stopPlayback()
        MusicService.stop(context)
        playingVideo = null
        showVideos = true; showFolders = false; showEq = false
        showPlaylists = false
        selectedFolder = null; selectedPlaylist = null
        selectedVideoFolder = null
    }

    // Video a pantalla completa (v1.3): SOLO en horizontal. En vertical
    // el video se ve arriba en tamaño normal, estilo YouTube (v1.4),
    // con la lista debajo; solo al girar a horizontal (o pulsar
    // pantalla completa) se expande a toda la pantalla.
    if (playingVideo != null && isLandscape) {
        VideoPlayerScreen(
            isLandscape = isLandscape,
            isInPip = isInPip,
            onClose = { closeVideoScreen() },
            onStop = { stopVideoScreen() },
            onRotate = { if (isLandscape) rotateToPortrait() else rotateToLandscape() }
        )
        return
    }

    // En horizontal, la visualización ocupa TODA la pantalla: nada de
    // listas ni encabezado, solo el visualizador con sus controles.
    if (isLandscape) {
        FullscreenVisualizer(
            song = currentSong,
            isPlaying = isPlaying,
            style = vizStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffle = shuffle,
            repeatMode = repeatMode,
            onToggle = { PlayerManager.togglePlayPause(context) },
            onNext = { PlayerManager.next(context) },
            onPrevious = { PlayerManager.previous(context) },
            onSeek = { PlayerManager.seekTo(it) },
            onStop = {
                PlayerManager.stopPlayback()
                MusicService.stop(context)
            },
            onToggleShuffle = { PlayerManager.toggleShuffle() },
            onCycleRepeat = { PlayerManager.cycleRepeat() },
            onRotate = { rotateToPortrait() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        // Video en vertical (v1.4): estilo YouTube, arriba en tamaño
        // normal (16:9) con la lista debajo. Solo en horizontal (o con
        // el botón de pantalla completa) se expande a toda la pantalla.
        if (playingVideo != null && !isLandscape) {
            VideoInlinePlayer(
                onClose = { closeVideoScreen() },
                onStop = { stopVideoScreen() },
                onFullscreen = { rotateToLandscape() }
            )
        }
        // La lista ocupa toda la altura restante (weight 1f): el encabezado
        // y los controles van DENTRO de la LazyColumn, así que al bajar se
        // esconden y la lista de canciones ocupa toda la pantalla. Al subir
        // hasta arriba reaparecen. Es el mismo arreglo de Biblia SL 1.2.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ---- Encabezado + controles (se esconden al deslizar) ----
            item {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shenlong Play Music",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                            Text(
                                text = if (showVideos) {
                                    if (videos.isEmpty()) "Sin videos todavía"
                                    else "${videos.size} videos en tu teléfono"
                                } else if (songs.isEmpty()) "Sin canciones todavía"
                                else "${songs.size} canciones en tu teléfono",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        // Ajustes: engranaje que abre un menú desplegable con
                        // todas las vistas (Canciones, Carpetas, Listas y
                        // Ecualizador), para no tener la fila de botones
                        // grandes en pantalla.
                        Box {
                            TransportIconButton(
                                icon = Icons.Default.Settings,
                                description = "Ajustes",
                                color = NeonPurple,
                                selected = showSettingsMenu,
                                onClick = { showSettingsMenu = !showSettingsMenu }
                            )
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas las canciones", fontSize = 14.sp) },
                                    onClick = {
                                        showFolders = false; showEq = false; showPlaylists = false
                                        showVideos = false
                                        selectedFolder = null; selectedPlaylist = null
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Carpetas", fontSize = 14.sp) },
                                    onClick = {
                                        showFolders = true; showEq = false; showPlaylists = false
                                        showVideos = false
                                        selectedPlaylist = null
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Listas de reproducción", fontSize = 14.sp) },
                                    onClick = {
                                        showPlaylists = true; showFolders = false; showEq = false
                                        showVideos = false
                                        selectedFolder = null
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Videos", fontSize = 14.sp) },
                                    onClick = {
                                        showVideos = true; showFolders = false; showEq = false
                                        showPlaylists = false
                                        selectedFolder = null; selectedPlaylist = null
                                        selectedVideoFolder = null
                                        showSettingsMenu = false
                                        if (hasVideoPermission) refreshVideos()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Color de tarjetas", fontSize = 14.sp) },
                                    onClick = {
                                        showCardColorDialog = true
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Color de fondo", fontSize = 14.sp) },
                                    onClick = {
                                        showBgColorDialog = true
                                        showSettingsMenu = false
                                    }
                                )
                                // Giro de pantalla (v1.3): interruptor dentro
                                // del menú. Apagado, la app queda fija en
                                // vertical; encendido, gira como siempre.
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Permitir giro de pantalla",
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = rotationAllowed,
                                                onCheckedChange = { checked ->
                                                    rotationAllowed = checked
                                                    saveRotation()
                                                }
                                            )
                                        }
                                    },
                                    onClick = {
                                        rotationAllowed = !rotationAllowed
                                        saveRotation()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Visualización", fontSize = 14.sp) },
                                    onClick = {
                                        showVizStyleDialog = true
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ecualizador", fontSize = 14.sp) },
                                    onClick = {
                                        showEq = true; showFolders = false; showPlaylists = false
                                        showVideos = false
                                        selectedPlaylist = null
                                        showSettingsMenu = false
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Lupa: icono vectorial real (Icons.Default.Search),
                        // botón de icono compacto de tamaño fijo: 48 dp en
                        // total, icono de 24 dp dentro, sin estirar. Al
                        // pulsarla se expande la barra de búsqueda; al
                        // cerrarla vuelve a quedar solo la lupa.
                        TransportIconButton(
                            icon = Icons.Default.Search,
                            description = "Buscar",
                            color = NeonBlue,
                            selected = showSearch,
                            onClick = {
                                showSearch = !showSearch
                                if (!showSearch) query = ""
                            }
                        )
                        // Giro de pantalla: en vertical gira a horizontal
                        // para ver la visualización a pantalla completa.
                        // Si el giro está desactivado en Ajustes, el botón
                        // no aparece (v1.3).
                        if (rotationAllowed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TransportIconButton(
                                icon = Icons.Default.ScreenRotation,
                                description = "Ver a pantalla completa",
                                color = NeonGreen,
                                selected = false,
                                onClick = { rotateToLandscape() }
                            )
                        }
                        // Cambio rápido música / video (v1.3): un solo
                        // toque cambia entre la lista de música y la de
                        // videos, sin entrar al menú de ajustes. Al
                        // cambiar se DETIENE por completo lo que esté
                        // sonando (v1.4): no es pausa, se para todo y se
                        // quita la notificación. Para ver un video se
                        // pulsa en la lista.
                        Spacer(modifier = Modifier.width(6.dp))
                        TransportIconButton(
                            icon = if (showVideos) Icons.Default.MusicNote else Icons.Default.Videocam,
                            description = if (showVideos) "Ver música" else "Ver videos",
                            color = NeonPink,
                            selected = showVideos,
                            onClick = {
                                PlayerManager.stopPlayback()
                                MusicService.stop(context)
                                playingVideo = null
                                pipVideoScreenOpen = false
                                showSearch = false; query = ""
                                if (showVideos) {
                                    showVideos = false
                                    selectedFolder = null; selectedPlaylist = null
                                    selectedVideoFolder = null
                                } else {
                                    showVideos = true; showFolders = false; showEq = false
                                    showPlaylists = false
                                    selectedFolder = null; selectedPlaylist = null
                                    selectedVideoFolder = null
                                    if (hasVideoPermission) refreshVideos()
                                }
                            }
                        )
                        // Transmitir a TV (v1.4): botón Cast de Google.
                        // Busca Chromecast, TV con Google TV o TV box en
                        // la misma WiFi y manda lo que suena a la TV.
                        Spacer(modifier = Modifier.width(6.dp))
                        CastButton()
                    }
                    if (showSearch) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                label = { Text("Buscar canción, artista o álbum", fontSize = 12.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TransportIconButton(
                                icon = Icons.Default.Close,
                                description = "Cerrar búsqueda",
                                color = NeonRed,
                                selected = false,
                                onClick = { showSearch = false; query = "" }
                            )
                        }
                    }
                    if (hasPermission && !hasNotifPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sin el permiso de notificaciones la música sigue " +
                                "sonando, pero no salen los controles en la pantalla " +
                                "de bloqueo ni en la barra de notificaciones.",
                            fontSize = 12.sp,
                            color = NeonRed
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { permissionLauncher.launch(permissionsToRequest()) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Dar permiso de notificaciones", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ---- Canción sonando + controles de reproducción ----
            if (currentSong != null && !showEq) {
                item {
                    NowPlayingCard(
                        cardColor = cardBg,
                        style = vizStyle,
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        shuffle = shuffle,
                        repeatMode = repeatMode,
                        onToggle = { PlayerManager.togglePlayPause(context) },
                        onNext = { PlayerManager.next(context) },
                        onPrevious = { PlayerManager.previous(context) },
                        onSeek = { PlayerManager.seekTo(it) },
                        onToggleShuffle = { PlayerManager.toggleShuffle() },
                        onCycleRepeat = { PlayerManager.cycleRepeat() },
                        onStop = {
                            PlayerManager.stopPlayback()
                            MusicService.stop(context)
                        }
                    )
                }
            }

            // ---- Video sonando en segundo plano (v1.3): el audio sigue
            // aunque se haya cerrado la pantalla de video. Al pulsar la
            // tarjeta se vuelve a abrir el video y se retoma la imagen.
            if (currentVideo != null && playingVideo == null && !showEq) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .background(cardBg, RoundedCornerShape(12.dp))
                            .clickable { playingVideo = currentVideo }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Video en segundo plano",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                            Text(
                                text = "${currentVideo!!.title} • toca para ver el video",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        ActionIconButton(
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            description = if (isPlaying) "Pausa" else "Reproducir",
                            color = NeonPurple,
                            onClick = { PlayerManager.togglePlayPause(context) }
                        )
                        ActionIconButton(
                            icon = Icons.Default.Stop,
                            description = "Detener por completo",
                            color = NeonRed,
                            onClick = {
                                PlayerManager.stopPlayback()
                                MusicService.stop(context)
                            }
                        )
                    }
                }
            }

            // ---- Ecualizador ----
            if (showEq) {
                item {
                    EqualizerPanel(
                        cardColor = cardBg,
                        enabled = eqEnabled,
                        preset = eqPreset,
                        bands = eqBands,
                        bass = eqBass,
                        onEnabledChange = { enabled ->
                            eqEnabled = enabled
                            EqManager.setEnabled(enabled)
                            if (enabled) {
                                EqManager.applyLevels(eqBands)
                                EqManager.setBass(eqBass)
                            }
                            saveEq()
                        },
                        onPresetChange = { name ->
                            eqPreset = name
                            eqBands = EqPresets[name] ?: eqBands
                            if (eqEnabled) EqManager.applyLevels(eqBands)
                            saveEq()
                        },
                        onBandChange = { index, value ->
                            val updated = eqBands.toMutableList()
                            updated[index] = value
                            eqBands = updated
                            eqPreset = "Personal"
                            if (eqEnabled) EqManager.applyLevels(eqBands)
                            saveEq()
                        },
                        onBassChange = { value ->
                            eqBass = value
                            if (eqEnabled) EqManager.setBass(value)
                            saveEq()
                        }
                    )
                }
            }

            // ---- Permiso / lista vacía ----
            if (!hasPermission) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Para ver tu música, la app necesita permiso para leer audio.",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(permissionsToRequest()) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Dar permiso para ver mi música", fontSize = 12.sp)
                        }
                    }
                }
            } else if (songs.isEmpty()) {
                item {
                    Text(
                        text = "No se encontró música en el teléfono. " +
                            "Copia canciones (MP3, FLAC, WAV…) a tu teléfono y vuelve a abrir la app.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ---- Carpetas ----
            if (showFolders && !showEq) {
                if (selectedFolder == null) {
                    items(folders) { (folder, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .background(cardBg, RoundedCornerShape(12.dp))
                                .clickable { selectedFolder = folder }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonGreen, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = File(folder).name.ifBlank { folder },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "$count canciones", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            // Ocultar carpeta: desaparece de la lista de
                            // canciones y de carpetas; se puede volver a
                            // mostrar en "Carpetas ocultas".
                            ActionIconButton(
                                icon = Icons.Default.VisibilityOff,
                                description = "Ocultar carpeta",
                                color = NeonRed,
                                onClick = { saveHiddenFolders(hiddenFolders + folder) }
                            )
                        }
                    }
                } else {
                    item {
                        CompactButton(
                            text = "← Volver a carpetas",
                            color = NeonBlue,
                            selected = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = { selectedFolder = null }
                        )
                    }
                }
                // ---- Carpetas ocultas: se vuelven a mostrar una por una ----
                if (selectedFolder == null && hiddenFolders.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHiddenFolders = !showHiddenFolders }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (showHiddenFolders) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Carpetas ocultas (${hiddenFolders.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (showHiddenFolders) "Ocultar" else "Ver",
                                fontSize = 12.sp,
                                color = NeonPurple
                            )
                        }
                    }
                    if (showHiddenFolders) {
                        items(hiddenFolders.sorted()) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                                    .background(cardBg, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = File(folder).name.ifBlank { folder },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Oculta", fontSize = 12.sp, color = Color.DarkGray)
                                }
                                ActionIconButton(
                                    icon = Icons.Default.Visibility,
                                    description = "Mostrar carpeta",
                                    color = NeonGreen,
                                    onClick = { saveHiddenFolders(hiddenFolders - folder) }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Listas de reproducción ----
            if (showPlaylists && !showEq) {
                if (selectedPlaylist == null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Nombre de la lista nueva", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            TransportIconButton(
                                icon = Icons.Default.Add,
                                description = "Crear lista",
                                color = NeonPurple,
                                selected = false,
                                onClick = {
                                    val name = newPlaylistName.trim()
                                    if (name.isNotBlank() && playlists.none { it.name == name }) {
                                        savePlaylists(playlists + Playlist(name, emptyList()))
                                        newPlaylistName = ""
                                    }
                                }
                            )
                        }
                    }
                    if (playlists.isEmpty()) {
                        item {
                            Text(
                                text = "Aún no tienes listas. Escribe un nombre arriba " +
                                    "y pulsa + para crear una, luego agrega canciones " +
                                    "con el botón + de cada canción.",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(playlists) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .background(cardBg, RoundedCornerShape(12.dp))
                                .clickable { selectedPlaylist = pl.name }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonPurple, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = pl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${pl.songIds.size} canciones",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                            ActionIconButton(
                                icon = Icons.Default.PlayArrow,
                                description = "Reproducir lista",
                                color = NeonGreen,
                                onClick = {
                                    val listSongs = pl.songIds.mapNotNull { id ->
                                        songs.firstOrNull { it.id == id }
                                    }
                                    if (listSongs.isNotEmpty()) {
                                        PlayerManager.playSong(listSongs[0], listSongs, context)
                                    }
                                }
                            )
                            ActionIconButton(
                                icon = Icons.Default.Delete,
                                description = "Borrar lista",
                                color = NeonRed,
                                onClick = { savePlaylists(playlists.filter { it.name != pl.name }) }
                            )
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TransportIconButton(
                                icon = Icons.Default.ArrowBack,
                                description = "Volver a listas",
                                color = NeonBlue,
                                selected = false,
                                onClick = { selectedPlaylist = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedPlaylist ?: "",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${playlistSongs.size} canciones",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                            ActionIconButton(
                                icon = Icons.Default.PlayArrow,
                                description = "Reproducir lista",
                                color = NeonGreen,
                                onClick = {
                                    if (playlistSongs.isNotEmpty()) {
                                        PlayerManager.playSong(playlistSongs[0], playlistSongs, context)
                                    }
                                }
                            )
                        }
                    }
                    items(playlistSongs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .background(cardBg, RoundedCornerShape(12.dp))
                                .clickable { PlayerManager.playSong(song, playlistSongs, context) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (song == currentSong) FontWeight.Bold else FontWeight.Normal,
                                    color = if (song == currentSong) NeonPurple else Color.Black
                                )
                                Text(text = song.artist, fontSize = 12.sp, color = Color.DarkGray)
                            }
                            ActionIconButton(
                                icon = Icons.Default.Close,
                                description = "Quitar de la lista",
                                color = NeonRed,
                                onClick = {
                                    val updated = playlists.map { pl ->
                                        if (pl.name == selectedPlaylist) pl.copy(songIds = pl.songIds - song.id)
                                        else pl
                                    }
                                    savePlaylists(updated)
                                }
                            )
                            Text(text = formatTime(song.durationMs), fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // ---- Lista de canciones ----
            if (!showFolders && !showEq && !showPlaylists && !showVideos) {
                items(visibleSongs) { song ->
                    val neon = NeonColors[(song.id % NeonColors.size).toInt()]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .background(cardBg, RoundedCornerShape(12.dp))
                            .clickable {
                                PlayerManager.playSong(song, visibleSongs, context)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(neon, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♪",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = if (song == currentSong) FontWeight.Bold else FontWeight.Normal,
                                color = if (song == currentSong) NeonPurple else Color.Black
                            )
                            Text(
                                text = "${song.artist} • ${song.folderName}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        ActionIconButton(
                            icon = Icons.Default.PlaylistAdd,
                            description = "Agregar a lista",
                            color = NeonPurple,
                            onClick = { addToPlaylistSong = song }
                        )
                        Text(text = formatTime(song.durationMs), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
            // ---- Videos por carpetas (v1.4): igual que la música ----
            // En vez de una sola lista general, se muestran las carpetas
            // con su número de videos; al pulsar una carpeta salen sus
            // videos. Las carpetas se ocultan/muestran con las mismas
            // carpetas ocultas de la música.
            if (showVideos && !showEq) {
                if (!hasVideoPermission) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Para ver tus videos, la app necesita el permiso de videos.",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(permissionsToRequest()) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Dar permiso de videos", fontSize = 12.sp)
                            }
                        }
                    }
                } else if (videos.isEmpty()) {
                    item {
                        Text(
                            text = "No se encontraron videos en el teléfono. " +
                                "Copia videos (MP4, MKV, WebM, 3GP…) a tu teléfono y vuelve a abrir la app.",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (selectedVideoFolder == null) {
                    items(videoFolders) { (folder, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .background(cardBg, RoundedCornerShape(12.dp))
                                .clickable { selectedVideoFolder = folder }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonPink, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = File(folder).name.ifBlank { folder },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "$count videos", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            ActionIconButton(
                                icon = Icons.Default.VisibilityOff,
                                description = "Ocultar carpeta",
                                color = NeonRed,
                                onClick = { saveHiddenFolders(hiddenFolders + folder) }
                            )
                        }
                    }
                    // Carpetas ocultas de videos (las mismas de música).
                    if (hiddenFolders.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showHiddenFolders = !showHiddenFolders }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (showHiddenFolders) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Carpetas ocultas (${hiddenFolders.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (showHiddenFolders) "Ocultar" else "Ver",
                                    fontSize = 12.sp,
                                    color = NeonPurple
                                )
                            }
                        }
                        if (showHiddenFolders) {
                            items(hiddenFolders.sorted()) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .background(cardBg, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = File(folder).name.ifBlank { folder },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = "Oculta", fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                    ActionIconButton(
                                        icon = Icons.Default.Visibility,
                                        description = "Mostrar carpeta",
                                        color = NeonGreen,
                                        onClick = { saveHiddenFolders(hiddenFolders - folder) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        CompactButton(
                            text = "← Volver a carpetas de videos",
                            color = NeonBlue,
                            selected = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = { selectedVideoFolder = null }
                        )
                    }
                    items(selectedVideoVideos) { video ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .background(cardBg, RoundedCornerShape(12.dp))
                                .clickable {
                                    PlayerManager.playVideo(context, video, selectedVideoVideos)
                                    playingVideo = video
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VideoThumbnail(video = video)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (video == currentVideo) FontWeight.Bold else FontWeight.Normal,
                                    color = if (video == currentVideo) NeonPurple else Color.Black
                                )
                                Text(
                                    text = "Video • ${formatTime(video.durationMs)}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                            Text(text = formatTime(video.durationMs), fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
            // Canciones de la carpeta elegida (reutiliza la misma lista).
            if (showFolders && selectedFolder != null && !showEq) {
                items(visibleSongs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .background(cardBg, RoundedCornerShape(12.dp))
                            .clickable { PlayerManager.playSong(song, visibleSongs, context) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = if (song == currentSong) FontWeight.Bold else FontWeight.Normal,
                                color = if (song == currentSong) NeonPurple else Color.Black
                            )
                            Text(text = song.artist, fontSize = 12.sp, color = Color.DarkGray)
                        }
                        ActionIconButton(
                            icon = Icons.Default.PlaylistAdd,
                            description = "Agregar a lista",
                            color = NeonPurple,
                            onClick = { addToPlaylistSong = song }
                        )
                        Text(text = formatTime(song.durationMs), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }

        // ---- Diálogo para agregar una canción a una lista ----
        if (addToPlaylistSong != null) {
            val songToAdd = addToPlaylistSong!!
            AlertDialog(
                onDismissRequest = { addToPlaylistSong = null },
                title = { Text("Agregar a lista", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(songToAdd.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        playlists.forEach { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        savePlaylists(playlists.map {
                                            if (it.name == pl.name) it.copy(
                                                songIds = if (songToAdd.id in it.songIds) it.songIds
                                                else it.songIds + songToAdd.id
                                            ) else it
                                        })
                                        addToPlaylistSong = null
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlaylistAdd,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = pl.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(
                                    text = "${pl.songIds.size} canciones",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        if (playlists.isEmpty()) {
                            Text(
                                text = "Aún no tienes listas. Crea una abajo.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Nueva lista", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank()) {
                            savePlaylists(playlists + Playlist(name, listOf(songToAdd.id)))
                            newPlaylistName = ""
                            addToPlaylistSong = null
                        }
                    }) { Text("Crear y agregar", fontSize = 12.sp) }
                },
                dismissButton = {
                    TextButton(onClick = { addToPlaylistSong = null }) {
                        Text("Cancelar", fontSize = 12.sp)
                    }
                }
            )
        }

        // ---- Diálogo de color de tarjetas: color fijo + transparencia ----
        if (showCardColorDialog) {
            AlertDialog(
                onDismissRequest = { showCardColorDialog = false },
                title = { Text("Color de tarjetas", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Elige un color fijo:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Fila con desplazamiento horizontal (v1.3): ahora
                        // hay 17 colores y ninguno se corta; se desliza
                        // para ver los que siguen (amarillo, morado, negro…).
                        ColorPickerRow(
                            selectedName = cardColorName,
                            onSelect = { name ->
                                cardColorName = name
                                saveCardColor()
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Color: $cardColorName", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transparencia: ${(cardTransparency * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0% = tarjeta opaca, 100% = casi transparente.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Slider(
                            value = cardTransparency,
                            onValueChange = { cardTransparency = it },
                            onValueChangeFinished = { saveCardColor() },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Vista previa con el color y la transparencia elegidos.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(cardBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Así se ven las tarjetas", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        saveCardColor()
                        showCardColorDialog = false
                    }) { Text("Listo", fontSize = 12.sp) }
                }
            )
        }

        // ---- Diálogo de color de fondo: igual que el de tarjetas ----
        if (showBgColorDialog) {
            AlertDialog(
                onDismissRequest = { showBgColorDialog = false },
                title = { Text("Color de fondo", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Elige un color fijo:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ColorPickerRow(
                            selectedName = bgColorName,
                            onSelect = { name ->
                                bgColorName = name
                                saveBgColor()
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Color: $bgColorName", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transparencia: ${(bgTransparency * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "0% = fondo opaco, 100% = casi transparente.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Slider(
                            value = bgTransparency,
                            onValueChange = { bgTransparency = it },
                            onValueChangeFinished = { saveBgColor() },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Vista previa con el color y la transparencia elegidos.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(appBg, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Así se ve el fondo", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        saveBgColor()
                        showBgColorDialog = false
                    }) { Text("Listo", fontSize = 12.sp) }
                }
            )
        }

        // ---- Diálogo de estilo de visualización ----
        if (showVizStyleDialog) {
            AlertDialog(
                onDismissRequest = { showVizStyleDialog = false },
                title = { Text("Visualización", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Elige cómo se ve la música:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        VisualizerStyleOptions.forEach { (name, description) ->
                            val selected = name == vizStyle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vizStyle = name
                                        saveVizStyle()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            2.dp,
                                            if (selected) NeonPurple else Color.Gray,
                                            CircleShape
                                        )
                                        .background(
                                            if (selected) NeonPurple else Color.Transparent,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = description,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        saveVizStyle()
                        showVizStyleDialog = false
                    }) { Text("Listo", fontSize = 12.sp) }
                }
            )
        }
    }
}
// ---------------------------------------------------------------------------
// Botón compacto: 36 dp de alto, letra 12 sp, peso igual en la fila.
// ---------------------------------------------------------------------------

@Composable
fun CompactButton(
    text: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else color.copy(alpha = 0.25f),
            contentColor = if (selected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = text, fontSize = 12.sp, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Botón de icono compacto: tamaño fijo, sin estirar.
// TransportIconButton: 48 dp en total, icono de 24 dp dentro, centrado.
// Se usa para reproducción (anterior, play/pausa, siguiente), aleatorio,
// repetir y la lupa: quedan al tamaño del icono, no enormes.
// ActionIconButton: 36 dp en total, icono de 20 dp, para acciones de fila
// (agregar a lista, ocultar/mostrar carpeta, borrar, quitar).
// ---------------------------------------------------------------------------

@Composable
fun TransportIconButton(
    icon: ImageVector,
    description: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                if (selected) color.copy(alpha = 0.20f) else Color.Transparent,
                RoundedCornerShape(24.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) color else Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Visualizador estilo Windows Media Player: 28 barras verticales de
// colores neón sobre fondo oscuro, que suben y bajan al ritmo real de
// la música (FFT del Visualizer). En pausa quedan en mínimo; si el
// Visualizer no existe en el dispositivo, se animan suaves como respaldo.
// ---------------------------------------------------------------------------

@Composable
fun VisualizerBars(
    isPlaying: Boolean,
    style: String = "Barras neón",
    fullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val levels by VisualizerManager.levels.collectAsState()
    val fallback by VisualizerManager.fallback.collectAsState()

    // Tiempo animado solo para el modo de respaldo (sin Visualizer real).
    val transition = rememberInfiniteTransition(label = "visualizer-fallback")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Restart
        ),
        label = "visualizer-time"
    )

    // Nivel de cada barra (0.06 a 1.0): real por FFT, o animado suave si
    // el Visualizer no existe en el dispositivo.
    fun levelAt(i: Int): Float {
        return if (fallback) {
            if (isPlaying) {
                (0.25f + 0.30f * sin(time + i * 0.55f).toFloat() +
                    0.18f * sin(time * 1.7f + i * 1.3f).toFloat())
                    .coerceIn(0.06f, 1f)
            } else {
                0.06f
            }
        } else {
            levels.getOrElse(i) { 0.06f }
        }
    }

    val canvasModifier = if (fullscreen) {
        modifier.fillMaxSize().background(Color.Black)
    } else {
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color(0xFF101018), RoundedCornerShape(12.dp))
    }
    Canvas(modifier = canvasModifier) {
        val count = VisualizerManager.BAR_COUNT
        when (style) {
            "Onda" -> {
                // Línea de onda continua (con su espejo tenue abajo).
                val midY = size.height / 2f
                val amp = size.height * 0.45f
                var prevTop: Offset? = null
                var prevBottom: Offset? = null
                for (i in 0 until count) {
                    val x = if (count > 1) size.width * i / (count - 1) else 0f
                    val yTop = midY - levelAt(i) * amp
                    val yBottom = midY + levelAt(i) * amp
                    val top = Offset(x, yTop)
                    val bottom = Offset(x, yBottom)
                    if (prevTop != null && prevBottom != null) {
                        drawLine(
                            color = NeonColors[i % NeonColors.size],
                            start = prevTop,
                            end = top,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = NeonColors[i % NeonColors.size].copy(alpha = 0.35f),
                            start = prevBottom,
                            end = bottom,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    prevTop = top
                    prevBottom = bottom
                }
            }
            "Círculo" -> {
                // Barras en círculo alrededor del centro.
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseR = minOf(size.width, size.height) * 0.16f
                val maxLen = minOf(size.width, size.height) * 0.32f
                val stroke = (minOf(size.width, size.height) / count * 0.55f)
                    .coerceAtLeast(4.dp.toPx())
                for (i in 0 until count) {
                    val angle = (2 * PI * i / count - PI / 2).toFloat()
                    val dir = Offset(cos(angle), sin(angle))
                    val start = center + dir * baseR
                    val end = center + dir * (baseR + levelAt(i) * maxLen)
                    drawLine(
                        color = NeonColors[i % NeonColors.size],
                        start = start,
                        end = end,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            "Espejo" -> {
                // Barras espejo con puntos: arriba y abajo del centro.
                val midY = size.height / 2f
                val barWidth = size.width / count
                val gap = 2.dp.toPx()
                for (i in 0 until count) {
                    val level = levelAt(i)
                    val halfH = (level * size.height / 2f).coerceAtLeast(2.dp.toPx())
                    val color = NeonColors[i % NeonColors.size]
                    val x = i * barWidth + gap / 2
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, midY - halfH),
                        size = Size(barWidth - gap, halfH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = color.copy(alpha = 0.6f),
                        topLeft = Offset(x, midY),
                        size = Size(barWidth - gap, halfH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawCircle(
                        color = color,
                        radius = 3.dp.toPx(),
                        center = Offset(x + (barWidth - gap) / 2, midY - halfH)
                    )
                }
            }
            else -> {
                // "Barras neón": 28 barras verticales de colores neón.
                val barWidth = size.width / count
                val gap = 2.dp.toPx()
                for (i in 0 until count) {
                    val level = levelAt(i)
                    val barHeight = (level * size.height).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = NeonColors[i % NeonColors.size],
                        topLeft = Offset(
                            x = i * barWidth + gap / 2,
                            y = size.height - barHeight
                        ),
                        size = Size(barWidth - gap, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenVisualizer(
    song: Song?,
    isPlaying: Boolean,
    style: String,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: Int,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onStop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRotate: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    // Los controles se esconden solos a los 3 segundos; al pulsar
    // cualquier lugar de la pantalla vuelven a aparecer.
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        VisualizerBars(
            isPlaying = isPlaying,
            style = style,
            fullscreen = true,
            modifier = Modifier.fillMaxSize()
        )
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: "Shenlong Play Music",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = song?.let { "${it.artist} • ${it.album}" }
                                ?: "Sin canción sonando",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    // En horizontal, el mismo giro vuelve a vertical.
                    TransportIconButton(
                        icon = Icons.Default.ScreenRotation,
                        description = "Volver a vertical",
                        color = Color.White,
                        selected = true,
                        onClick = onRotate
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Slider(
                    value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                    onValueChange = { fraction ->
                        if (durationMs > 0) onSeek((fraction * durationMs).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(positionMs), fontSize = 12.sp, color = Color.White)
                    Text(text = formatTime(durationMs), fontSize = 12.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Controles a pantalla completa (v1.4): los 6 iconos —
                // aleatorio, anterior, play/pausa, stop, siguiente y
                // repetir — todos de 48 dp, con el estado activo visible
                // (aleatorio naranja y repetir rojo al activarse).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportIconButton(
                        icon = Icons.Default.Shuffle,
                        description = if (shuffle) "Aleatorio activado" else "Aleatorio",
                        color = NeonOrange,
                        selected = shuffle,
                        onClick = onToggleShuffle
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TransportIconButton(
                        icon = Icons.Default.SkipPrevious,
                        description = "Anterior",
                        color = NeonBlue,
                        selected = true,
                        onClick = onPrevious
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TransportIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        description = if (isPlaying) "Pausa" else "Reproducir",
                        color = NeonPurple,
                        selected = true,
                        onClick = onToggle
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TransportIconButton(
                        icon = Icons.Default.Stop,
                        description = "Detener por completo",
                        color = NeonRed,
                        selected = true,
                        onClick = onStop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TransportIconButton(
                        icon = Icons.Default.SkipNext,
                        description = "Siguiente",
                        color = NeonBlue,
                        selected = true,
                        onClick = onNext
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TransportIconButton(
                        icon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        description = PlayerManager.repeatLabel(),
                        color = NeonRed,
                        selected = repeatMode != 0,
                        onClick = onCycleRepeat
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Miniatura de video (MediaStore, v1.3): se carga perezosa por fila con
// loadThumbnail (Android 9+); si no hay miniatura, icono de video.
// ---------------------------------------------------------------------------

@Composable
fun VideoThumbnail(video: VideoItem) {
    val context = LocalContext.current
    var bitmap by remember(video.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(video.id) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(video.uri, android.util.Size(160, 90), null)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp, 36.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .size(64.dp, 36.dp)
                .background(NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Botón Cast (v1.4): botón de Google Cast (MediaRouteButton) que busca
// Chromecast, TV con Google TV o TV box en la misma WiFi. Al elegir un
// dispositivo, CastManager manda lo que suena a la TV. Si el teléfono
// no tiene Google Play Services, el botón no muestra dispositivos y la
// app sigue funcionando igual.
// ---------------------------------------------------------------------------

@Composable
fun CastButton() {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            MediaRouteButton(ctx).apply {
                try {
                    routeSelector =
                        CastContext.getSharedInstance(ctx.applicationContext).mergedSelector
                } catch (e: Exception) {
                    // Sin Google Play Services no hay dispositivos Cast.
                }
            }
        },
        modifier = Modifier.size(48.dp)
    )
}

// ---------------------------------------------------------------------------
// Video en vertical estilo YouTube (v1.4): el video arriba en tamaño
// normal (16:9) con sus controles debajo, y la lista de videos debajo
// en la pantalla principal. Solo al girar a horizontal (o pulsar
// pantalla completa) se expande a toda la pantalla.
// ---------------------------------------------------------------------------

@Composable
fun VideoInlinePlayer(
    onClose: () -> Unit,
    onStop: () -> Unit,
    onFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val video by PlayerManager.currentVideo.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val positionMs by PlayerManager.positionMs.collectAsState()
    val durationMs by PlayerManager.durationMs.collectAsState()
    val isInPip by MainActivity.isInPipFlow.collectAsState()
    val isCasting by CastManager.isCasting.collectAsState()
    val castDevice by CastManager.castDeviceName.collectAsState()
    BackHandler { onClose() }
    // Al volver de la ventanita PiP (v1.4): se vuelve a enganchar la
    // imagen, que si no solo sonaría el audio.
    LaunchedEffect(isInPip) {
        if (!isInPip) PlayerManager.reattachVideoSurface()
    }
    DisposableEffect(Unit) {
        onDispose { PlayerManager.setVideoSurface(null) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            PlayerManager.setVideoSurface(this@apply)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {}
                    })
                }
            },
            update = { view -> PlayerManager.setVideoSurface(view) },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video?.title ?: "Video",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isCasting) "Transmitiendo a ${castDevice ?: "TV"}"
                        else "Video • ${formatTime(durationMs)}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
                CastButton()
                Spacer(modifier = Modifier.width(6.dp))
                TransportIconButton(
                    icon = Icons.Default.Fullscreen,
                    description = "Pantalla completa",
                    color = Color.White,
                    selected = true,
                    onClick = onFullscreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                TransportIconButton(
                    icon = Icons.Default.Close,
                    description = "Cerrar video",
                    color = Color.White,
                    selected = true,
                    onClick = onClose
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                onValueChange = { fraction ->
                    if (durationMs > 0) PlayerManager.seekTo((fraction * durationMs).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(positionMs), fontSize = 12.sp, color = Color.White)
                Text(text = formatTime(durationMs), fontSize = 12.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportIconButton(
                    icon = Icons.Default.SkipPrevious,
                    description = "Video anterior",
                    color = NeonBlue,
                    selected = true,
                    onClick = { PlayerManager.previous(context) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TransportIconButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = if (isPlaying) "Pausa" else "Reproducir",
                    color = NeonPurple,
                    selected = true,
                    onClick = { PlayerManager.togglePlayPause(context) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                TransportIconButton(
                    icon = Icons.Default.Stop,
                    description = "Detener por completo",
                    color = NeonRed,
                    selected = true,
                    onClick = onStop
                )
                Spacer(modifier = Modifier.width(16.dp))
                TransportIconButton(
                    icon = Icons.Default.SkipNext,
                    description = "Video siguiente",
                    color = NeonBlue,
                    selected = true,
                    onClick = { PlayerManager.next(context) }
                )
            }
            Text(
                text = "Gira el teléfono o pulsa pantalla completa para " +
                    "verlo a lo grande. Al cerrar, el audio sigue en " +
                    "segundo plano; el botón de stop lo detiene todo.",
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pantalla de video a pantalla completa (v1.3): el video se dibuja en un
// SurfaceView sobre el MISMO ExoPlayer de la música, así que al salir de
// esta pantalla (o apagar la pantalla, o minimizar la app) el audio sigue
// sonando por el servicio en primer plano, con los controles en la
// notificación. Al volver a abrirla, la superficie se vuelve a enganchar
// y se retoma la imagen. Al pulsar en cualquier lugar aparecen o se
// esconden los controles (como en la visualización a pantalla completa),
// que también se esconden solos a los 3 segundos.
// ---------------------------------------------------------------------------

@Composable
fun VideoPlayerScreen(
    isLandscape: Boolean,
    isInPip: Boolean = false,
    onClose: () -> Unit,
    onStop: () -> Unit,
    onRotate: () -> Unit
) {
    val context = LocalContext.current
    val video by PlayerManager.currentVideo.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val positionMs by PlayerManager.positionMs.collectAsState()
    val durationMs by PlayerManager.durationMs.collectAsState()
    val isCasting by CastManager.isCasting.collectAsState()
    val castDevice by CastManager.castDeviceName.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    // El botón de atrás del teléfono también cierra la pantalla (el
    // audio sigue en segundo plano, como al pulsar la X). En la
    // ventanita PiP no hay botón de atrás: se sale pulsando la ventana.
    BackHandler(enabled = !isInPip) { onClose() }
    LaunchedEffect(showControls, isInPip) {
        if (showControls && !isInPip) {
            delay(3000)
            showControls = false
        }
    }
    // Al salir de la pantalla se suelta la superficie (se quita la
    // imagen) pero el reproductor NO se detiene: el audio sigue.
    DisposableEffect(Unit) {
        onDispose { PlayerManager.setVideoSurface(null) }
    }
    // Al volver de la ventanita PiP (v1.4): se vuelve a enganchar la
    // imagen, que si no solo sonaría el audio.
    LaunchedEffect(isInPip) {
        if (!isInPip) PlayerManager.reattachVideoSurface()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    // Si Android suelta y vuelve a crear la superficie
                    // (por ejemplo al volver de PiP), se vuelve a
                    // enganchar sola (v1.4).
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            PlayerManager.setVideoSurface(this@apply)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {}
                    })
                }
            },
            update = { view -> PlayerManager.setVideoSurface(view) },
            modifier = Modifier.fillMaxSize()
        )
        // En la ventanita PiP no se dibujan los controles de pantalla
        // completa: la ventanita lleva solo su botón play/pausa y al
        // pulsarla se vuelve a pantalla completa. La superficie del
        // video sigue enganchada, así que la imagen sigue viéndose.
        if (showControls && !isInPip) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video?.title ?: "Video",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isCasting) "Transmitiendo a ${castDevice ?: "TV"}"
                            else "Video • ${formatTime(durationMs)}",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    // Transmitir a TV (v1.4).
                    CastButton()
                    Spacer(modifier = Modifier.width(6.dp))
                    TransportIconButton(
                        icon = Icons.Default.ScreenRotation,
                        description = "Girar pantalla",
                        color = Color.White,
                        selected = true,
                        onClick = onRotate
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TransportIconButton(
                        icon = Icons.Default.Close,
                        description = "Cerrar video",
                        color = Color.White,
                        selected = true,
                        onClick = onClose
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Slider(
                    value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                    onValueChange = { fraction ->
                        if (durationMs > 0) PlayerManager.seekTo((fraction * durationMs).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(positionMs), fontSize = 12.sp, color = Color.White)
                    Text(text = formatTime(durationMs), fontSize = 12.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportIconButton(
                        icon = Icons.Default.SkipPrevious,
                        description = "Video anterior",
                        color = NeonBlue,
                        selected = true,
                        onClick = { PlayerManager.previous(context) }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TransportIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        description = if (isPlaying) "Pausa" else "Reproducir",
                        color = NeonPurple,
                        selected = true,
                        onClick = { PlayerManager.togglePlayPause(context) }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TransportIconButton(
                        icon = Icons.Default.Stop,
                        description = "Detener por completo",
                        color = NeonRed,
                        selected = true,
                        onClick = onStop
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TransportIconButton(
                        icon = Icons.Default.SkipNext,
                        description = "Video siguiente",
                        color = NeonBlue,
                        selected = true,
                        onClick = { PlayerManager.next(context) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Si sales con el botón de inicio, el video sigue " +
                        "en una ventanita flotante. Si sales de esta " +
                        "pantalla o apagas la pantalla, el audio sigue " +
                        "sonando. Vuelve a la app y toca \"Ver video\" " +
                        "para retomar la imagen. El botón de stop detiene " +
                        "el video por completo.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun NowPlayingCard(
    cardColor: Color,
    style: String,
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: Int,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onStop: () -> Unit
) {
    val isCasting by CastManager.isCasting.collectAsState()
    val castDevice by CastManager.castDeviceName.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(cardColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = "Sonando ahora", fontSize = 12.sp, color = NeonPink, fontWeight = FontWeight.Bold)
        Text(text = song.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = "${song.artist} • ${song.album}", fontSize = 12.sp, color = Color.DarkGray)
        // Transmitiendo a la TV (v1.4): se ve en la tarjeta.
        if (isCasting) {
            Text(
                text = "Transmitiendo a ${castDevice ?: "TV"}",
                fontSize = 12.sp,
                color = NeonPurple,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Visualizador estilo Windows Media Player: barras neón al ritmo
        // de la música, encima de la barra de progreso y los controles.
        VisualizerBars(isPlaying = isPlaying, style = style)
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
            onValueChange = { fraction ->
                if (durationMs > 0) onSeek((fraction * durationMs).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(positionMs), fontSize = 12.sp)
            Text(text = formatTime(durationMs), fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Controles de reproducción (v1.3, v1.4): una sola fila de 6
        // iconos compactos — aleatorio, anterior, play/pausa, siguiente,
        // repetir y STOP — todos de 48 dp con icono de 24 dp, sin letras
        // y sin ocupar más espacio arriba. El giro quedó solo en el
        // encabezado. Stop detiene por completo (no es pausa).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportIconButton(
                icon = Icons.Default.Shuffle,
                description = if (shuffle) "Aleatorio activado" else "Aleatorio",
                color = NeonOrange,
                selected = shuffle,
                onClick = onToggleShuffle
            )
            Spacer(modifier = Modifier.width(8.dp))
            TransportIconButton(
                icon = Icons.Default.SkipPrevious,
                description = "Anterior",
                color = NeonBlue,
                selected = false,
                onClick = onPrevious
            )
            Spacer(modifier = Modifier.width(8.dp))
            TransportIconButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                description = if (isPlaying) "Pausa" else "Reproducir",
                color = NeonPurple,
                selected = true,
                onClick = onToggle
            )
            Spacer(modifier = Modifier.width(8.dp))
            TransportIconButton(
                icon = Icons.Default.SkipNext,
                description = "Siguiente",
                color = NeonBlue,
                selected = false,
                onClick = onNext
            )
            Spacer(modifier = Modifier.width(8.dp))
            TransportIconButton(
                icon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                description = PlayerManager.repeatLabel(),
                color = NeonRed,
                selected = repeatMode != 0,
                onClick = onCycleRepeat
            )
            Spacer(modifier = Modifier.width(8.dp))
            TransportIconButton(
                icon = Icons.Default.Stop,
                description = "Detener por completo",
                color = NeonRed,
                selected = false,
                onClick = onStop
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Fila de colores con desplazamiento horizontal (v1.3): los 17 colores
// fijos en círculos de 36 dp; se desliza para ver todos sin que ninguno
// se corte a la derecha. La usan los diálogos de color de tarjetas y
// de color de fondo.
// ---------------------------------------------------------------------------

@Composable
fun ColorPickerRow(
    selectedName: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardColorOptions.forEach { (name, color) ->
            val selected = name == selectedName
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) Color.Black else Color.Gray,
                        shape = CircleShape
                    )
                    .background(color, CircleShape)
                    .clickable { onSelect(name) }
            )
        }
    }
}

@Composable
fun EqualizerPanel(
    cardColor: Color,
    enabled: Boolean,
    preset: String,
    bands: List<Short>,
    bass: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPresetChange: (String) -> Unit,
    onBandChange: (Int, Short) -> Unit,
    onBassChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(cardColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Ecualizador", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        Text(
            text = if (enabled) "Encendido" else "Apagado",
            fontSize = 12.sp,
            color = if (enabled) NeonGreen else Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EqPresets.keys.forEachIndexed { index, name ->
                val color = NeonColors[index % NeonColors.size]
                CompactButton(
                    text = name,
                    color = color,
                    selected = preset == name,
                    onClick = { onPresetChange(name) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        bands.forEachIndexed { index, level ->
            val name = EqBandNames.getOrElse(index) { "Banda ${index + 1}" }
            Text(text = "$name: ${level / 100} dB", fontSize = 12.sp)
            Slider(
                value = level.toFloat(),
                onValueChange = { onBandChange(index, it.toInt().toShort()) },
                valueRange = -1500f..1500f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Refuerzo de bajos: $bass", fontSize = 12.sp)
        Slider(
            value = bass.toFloat(),
            onValueChange = { onBassChange(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "El preset Altavoces baja los graves y sube medios y agudos " +
                "para que el teléfono no se distorsione.",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}
