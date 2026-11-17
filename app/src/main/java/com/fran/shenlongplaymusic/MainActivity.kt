package com.fran.shenlongplaymusic

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.app.Activity
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
    "Azul" to NeonBlue,
    "Rosa" to NeonPink,
    "Rojo" to NeonRed,
    "Verde" to NeonGreen,
    "Naranja" to NeonOrange,
    "Amarillo" to NeonYellow,
    "Morado" to NeonPurple,
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
                    val idx = mediaItem?.mediaId?.toIntOrNull()
                    if (idx != null && idx in playlist.indices) {
                        currentIndex = idx
                        _currentSong.value = playlist[idx]
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

    fun togglePlayPause(context: Context) {
        val p = player ?: return
        if (p.isPlaying) p.pause() else {
            p.play()
            MusicService.start(context)
        }
    }

    fun next(context: Context) {
        val p = player ?: return
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

/** Permisos que se piden juntos al abrir la app: audio + notificaciones. */
fun permissionsToRequest(): Array<String> {
    val list = mutableListOf(audioPermission())
    notificationPermission()?.let { list.add(it) }
    return list.toTypedArray()
}

// ---------------------------------------------------------------------------
// Actividad principal
// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerManager.init(this)
        setContent { ShenlongPlayMusicApp() }
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
        pendingOrientation = Configuration.ORIENTATION_LANDSCAPE
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    fun rotateToPortrait() {
        pendingOrientation = Configuration.ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    val playlists by playlistStore.playlistsFlow.collectAsState(initial = emptyList())
    val hiddenFolders by playlistStore.hiddenFoldersFlow.collectAsState(initial = emptySet())

    var eqEnabled by remember { mutableStateOf(false) }
    var eqPreset by remember { mutableStateOf("Normal") }
    var eqBands by remember { mutableStateOf(listOf<Short>(0, 0, 0, 0, 0)) }
    var eqBass by remember { mutableStateOf(0) }

    val currentSong by PlayerManager.currentSong.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
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

    fun saveVizStyle() {
        scope.launch { visualizerSettings.save(vizStyle) }
    }

    // Color de tarjetas elegido + transparencia: 0% = opaco, 100% =
    // casi transparente (alpha 0). Se aplica a todas las tarjetas.
    val cardColor = CardColorOptions.firstOrNull { it.first == cardColorName }?.second ?: Color.White
    val cardBg = cardColor.copy(alpha = (1f - cardTransparency).coerceIn(0f, 1f))

    fun refreshSongs() {
        songs = loadSongs(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[audioPermission()] == true
        hasNotifPermission = notificationPermission()?.let { result[it] == true } ?: true
        if (hasPermission) refreshSongs()
    }

    // Al abrir la app se piden juntos audio + notificaciones (Android 13+).
    // Sin el de notificaciones la música sigue sonando, pero Android no
    // muestra los controles en la barra ni en la pantalla de bloqueo.
    LaunchedEffect(Unit) {
        if (!hasPermission || !hasNotifPermission) {
            permissionLauncher.launch(permissionsToRequest())
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) refreshSongs()
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

    // En horizontal, la visualización ocupa TODA la pantalla: nada de
    // listas ni encabezado, solo el visualizador con sus controles.
    if (isLandscape) {
        FullscreenVisualizer(
            song = currentSong,
            isPlaying = isPlaying,
            style = vizStyle,
            positionMs = positionMs,
            durationMs = durationMs,
            onToggle = { PlayerManager.togglePlayPause(context) },
            onNext = { PlayerManager.next(context) },
            onPrevious = { PlayerManager.previous(context) },
            onSeek = { PlayerManager.seekTo(it) },
            onRotate = { rotateToPortrait() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
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
                                text = if (songs.isEmpty()) "Sin canciones todavía"
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
                                        selectedFolder = null; selectedPlaylist = null
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Carpetas", fontSize = 14.sp) },
                                    onClick = {
                                        showFolders = true; showEq = false; showPlaylists = false
                                        selectedPlaylist = null
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Listas de reproducción", fontSize = 14.sp) },
                                    onClick = {
                                        showPlaylists = true; showFolders = false; showEq = false
                                        selectedFolder = null
                                        showSettingsMenu = false
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
                        Spacer(modifier = Modifier.width(6.dp))
                        // Giro de pantalla: en vertical gira a horizontal
                        // para ver la visualización a pantalla completa.
                        TransportIconButton(
                            icon = Icons.Default.ScreenRotation,
                            description = "Ver a pantalla completa",
                            color = NeonGreen,
                            selected = false,
                            onClick = { rotateToLandscape() }
                        )
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
                    if (!showEq) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Aleatorio y repetir: solo iconos, botones de
                        // tamaño fijo (48 dp, icono 24 dp), centrados en
                        // la fila, sin estirar a todo el ancho.
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
                                onClick = { PlayerManager.toggleShuffle() }
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            TransportIconButton(
                                icon = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                description = PlayerManager.repeatLabel(),
                                color = NeonRed,
                                selected = repeatMode != 0,
                                onClick = { PlayerManager.cycleRepeat() }
                            )
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
                        onToggle = { PlayerManager.togglePlayPause(context) },
                        onNext = { PlayerManager.next(context) },
                        onPrevious = { PlayerManager.previous(context) },
                        onSeek = { PlayerManager.seekTo(it) },
                        onRotate = { rotateToLandscape() }
                    )
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
            if (!showFolders && !showEq && !showPlaylists) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CardColorOptions.forEach { (name, color) ->
                                val selected = name == cardColorName
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.Black else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .background(color, CircleShape)
                                        .clickable {
                                            cardColorName = name
                                            saveCardColor()
                                        }
                                )
                            }
                        }
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
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransportIconButton(
                        icon = Icons.Default.SkipPrevious,
                        description = "Anterior",
                        color = NeonBlue,
                        selected = true,
                        onClick = onPrevious
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TransportIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        description = if (isPlaying) "Pausa" else "Reproducir",
                        color = NeonPurple,
                        selected = true,
                        onClick = onToggle
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TransportIconButton(
                        icon = Icons.Default.SkipNext,
                        description = "Siguiente",
                        color = NeonBlue,
                        selected = true,
                        onClick = onNext
                    )
                }
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
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRotate: () -> Unit
) {
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
        // Controles de reproducción: solo iconos, botones de tamaño fijo
        // (48 dp en total, icono de 24 dp dentro), centrados en la fila,
        // sin estirar a todo el ancho y sin letras.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportIconButton(
                icon = Icons.Default.SkipPrevious,
                description = "Anterior",
                color = NeonBlue,
                selected = false,
                onClick = onPrevious
            )
            Spacer(modifier = Modifier.width(24.dp))
            TransportIconButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                description = if (isPlaying) "Pausa" else "Reproducir",
                color = NeonPurple,
                selected = true,
                onClick = onToggle
            )
            Spacer(modifier = Modifier.width(24.dp))
            TransportIconButton(
                icon = Icons.Default.SkipNext,
                description = "Siguiente",
                color = NeonBlue,
                selected = false,
                onClick = onNext
            )
            Spacer(modifier = Modifier.width(24.dp))
            TransportIconButton(
                icon = Icons.Default.ScreenRotation,
                description = "Ver a pantalla completa",
                color = NeonGreen,
                selected = false,
                onClick = onRotate
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
