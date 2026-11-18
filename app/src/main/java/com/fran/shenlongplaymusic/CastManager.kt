package com.fran.shenlongplaymusic

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.ParcelFileDescriptor
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

// ---------------------------------------------------------------------------
// Transmisión a TV / TV box / Chromecast (Google Cast, v1.4)
//
// Cómo funciona: la TV no puede leer los archivos del teléfono, así que
// la app levanta un servidor HTTP mínimo SOLO en la red WiFi de casa
// (no en internet) y le pasa a la TV la dirección local del archivo
// (http://192.168.x.x:puerto/media?uri=...). La TV descarga el archivo
// directamente del teléfono, por la misma WiFi. Nada se sube a internet:
// el servidor solo escucha en la red local y solo sirve el archivo que
// está sonando, y se apaga al terminar la transmisión.
//
// Requisitos: el teléfono y la TV tienen que estar en la misma WiFi.
// Si el teléfono no tiene Google Play Services, el botón Cast no
// muestra dispositivos y todo lo demás sigue funcionando igual.
// ---------------------------------------------------------------------------

object CastManager {
    private var appContext: Context? = null
    private var server: LocalHttpServer? = null
    private var listenerRegistered = false

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()
    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (listenerRegistered) return
        try {
            val castContext = CastContext.getSharedInstance(appContext!!)
            castContext.sessionManager.addSessionManagerListener(
                object : SessionManagerListener<CastSession> {
                    override fun onSessionStarting(session: CastSession) {}
                    override fun onSessionStarted(session: CastSession, sessionId: String) {
                        _isCasting.value = true
                        _castDeviceName.value = session.castDevice?.friendlyName
                        castCurrent(appContext!!)
                    }

                    override fun onSessionStartFailed(session: CastSession, error: Int) {}
                    override fun onSessionEnding(session: CastSession) {}
                    override fun onSessionEnded(session: CastSession, error: Int) {
                        _isCasting.value = false
                        _castDeviceName.value = null
                        stopServer()
                    }

                    override fun onSessionResuming(session: CastSession, sessionId: String) {}
                    override fun onSessionResumeFailed(session: CastSession, error: Int) {}
                    override fun onSessionSuspended(session: CastSession, reason: Int) {
                        _isCasting.value = false
                    }
                },
                CastSession::class.java
            )
            listenerRegistered = true
        } catch (e: Exception) {
            // Sin Google Play Services (o sin Cast): la app sigue igual,
            // solo que el botón Cast no encuentra dispositivos.
        }
    }

    /** ¿Hay una sesión Cast activa con una TV? */
    fun isConnected(context: Context): Boolean {
        return try {
            CastContext.getSharedInstance(context.applicationContext)
                .sessionManager.currentCastSession?.isConnected == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Manda lo que está sonando (canción o video) a la TV y pausa el
     * reproductor local: el sonido y la imagen salen por la TV.
     * Si no hay nada sonando o no hay TV conectada, no hace nada.
     */
    fun castCurrent(context: Context) {
        val appCtx = context.applicationContext
        val video = PlayerManager.currentVideo.value
        val song = PlayerManager.currentSong.value
        val uri = video?.uri ?: song?.uri ?: return
        val title = video?.title ?: song?.title ?: "Shenlong Play Music"
        val isVideo = video != null
        val session = try {
            CastContext.getSharedInstance(appCtx).sessionManager.currentCastSession
        } catch (e: Exception) {
            null
        } ?: return
        val client = session.remoteMediaClient ?: return
        val server = ensureServer(appCtx)
        val url = server.urlFor(uri) ?: return
        val metadata = MediaMetadata(
            if (isVideo) MediaMetadata.MEDIA_TYPE_MOVIE
            else MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
        )
        metadata.putString(MediaMetadata.KEY_TITLE, title)
        if (!isVideo && song != null) {
            metadata.putString(MediaMetadata.KEY_ARTIST, song.artist)
        }
        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType(appCtx, uri, isVideo))
            .setMetadata(metadata)
            .build()
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setCurrentTime(PlayerManager.positionMs.value)
            .build()
        client.load(request)
        // El archivo ya suena en la TV: el teléfono se queda en pausa
        // para no sonar dos veces.
        PlayerManager.pause()
    }

    /** Vuelve a mandar a la TV lo que esté sonando (botón Cast). */
    fun recast(context: Context) {
        if (isConnected(context)) castCurrent(context)
    }

    private fun ensureServer(context: Context): LocalHttpServer {
        val existing = server
        if (existing != null) return existing
        return LocalHttpServer(context.applicationContext).also { server = it }
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    fun mimeType(context: Context, uri: Uri, isVideo: Boolean): String {
        val fromSystem = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        if (!fromSystem.isNullOrBlank()) return fromSystem
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".webm") -> if (isVideo) "video/webm" else "audio/webm"
            path.endsWith(".3gp") -> "video/3gpp"
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".m4a") -> "audio/mp4"
            path.endsWith(".flac") -> "audio/flac"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".ogg") -> "audio/ogg"
            path.endsWith(".opus") -> "audio/opus"
            path.endsWith(".mid") || path.endsWith(".midi") -> "audio/midi"
            path.endsWith(".amr") -> "audio/amr"
            isVideo -> "video/mp4"
            else -> "audio/mpeg"
        }
    }

    /** IP local del teléfono en la WiFi (para que la TV lo encuentre). */
    fun localIpAddress(context: Context): String? {
        // 1) Interfaces de red: la primera IPv4 privada que no sea loopback.
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val netIf = interfaces.nextElement()
                if (!netIf.isUp || netIf.isLoopback) continue
                val addresses = netIf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress &&
                        addr.isSiteLocalAddress
                    ) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Se intenta el plan B (WifiManager).
        }
        // 2) WifiManager (necesita ACCESS_WIFI_STATE).
        return try {
            @Suppress("DEPRECATION")
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ip = wifi?.connectionInfo?.ipAddress ?: 0
            if (ip == 0) null
            else "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Servidor HTTP mínimo en la red local (v1.4).
 *
 * Solo sirve el archivo que está sonando, solo por la WiFi de casa y
 * solo mientras dura la transmisión. Atiende peticiones GET con
 * "Range" (para que la TV pueda adelantar/atrasar sin bajar todo el
 * archivo de golpe) y responde 200 o 206 con el tipo de contenido
 * correcto. Un hilo por conexión; se apaga con stop().
 */
class LocalHttpServer(private val context: Context) {
    private val serverSocket = ServerSocket(0)
    val port: Int = serverSocket.localPort
    @Volatile
    private var running = true

    init {
        thread(start = true, isDaemon = true, name = "shenlong-cast-server") {
            while (running) {
                try {
                    val socket = serverSocket.accept()
                    thread(start = true, isDaemon = true) { handle(socket) }
                } catch (e: Exception) {
                    if (!running) break
                }
            }
        }
    }

    fun urlFor(uri: Uri): String? {
        val ip = CastManager.localIpAddress(context) ?: return null
        val encoded = URLEncoder.encode(uri.toString(), "UTF-8")
        return "http://$ip:$port/media?uri=$encoded"
    }

    fun stop() {
        running = false
        try {
            serverSocket.close()
        } catch (e: Exception) {
        }
    }

    private fun handle(socket: Socket) {
        var input: InputStream? = null
        try {
            socket.soTimeout = 15000
            val inputStream = socket.getInputStream()
            input = inputStream
            val out = BufferedOutputStream(socket.getOutputStream())
            // Lee la petición (primera línea + cabeceras) hasta la línea vacía.
            val requestLines = mutableListOf<String>()
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isBlank()) break
                requestLines.add(line!!)
                if (requestLines.size > 40) break
            }
            val firstLine = requestLines.firstOrNull() ?: ""
            val uriParam = firstLine.split(" ").getOrNull(1) ?: ""
            val rawUri = uriParam.substringAfter("uri=", "")
            if (rawUri.isBlank()) {
                writeSimpleResponse(out, 400, "Falta uri")
                return
            }
            val mediaUri = Uri.parse(URLDecoder.decode(rawUri, "UTF-8"))
            var rangeStart = 0L
            var rangeEnd: Long? = null
            for (header in requestLines) {
                if (header.startsWith("Range:", ignoreCase = true)) {
                    val range = header.substringAfter(":").trim()
                        .removePrefix("bytes=")
                    val parts = range.split("-")
                    rangeStart = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                    rangeEnd = parts.getOrNull(1)?.toLongOrNull()
                }
            }
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(mediaUri, "r")
                val fd = pfd?.fileDescriptor ?: run {
                    writeSimpleResponse(out, 404, "No se encontró el archivo")
                    return
                }
                val fileSize = try {
                    pfd.statSize
                } catch (e: Exception) {
                    -1L
                }
                val fis = FileInputStream(fd)
                val total = if (fileSize > 0) fileSize
                else fis.channel.size()
                val end = (rangeEnd ?: (total - 1)).coerceAtMost(total - 1)
                val length = (end - rangeStart + 1).coerceAtLeast(0)
                val isRange = rangeStart > 0 || rangeEnd != null
                val status = if (isRange) "206 Partial Content" else "200 OK"
                val mime = CastManager.mimeType(
                    context,
                    mediaUri,
                    mediaUri.toString().lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mkv") ||
                            it.endsWith(".webm") || it.endsWith(".3gp")
                    }
                )
                val headers = StringBuilder()
                headers.append("HTTP/1.1 $status\r\n")
                headers.append("Content-Type: $mime\r\n")
                headers.append("Accept-Ranges: bytes\r\n")
                headers.append("Content-Length: $length\r\n")
                if (isRange) {
                    headers.append("Content-Range: bytes $rangeStart-$end/$total\r\n")
                }
                headers.append("Connection: close\r\n\r\n")
                out.write(headers.toString().toByteArray(Charsets.UTF_8))
                out.flush()
                // Salta al punto pedido y copia solo lo que falta.
                fis.channel.position(rangeStart)
                val buffer = ByteArray(8192)
                var remaining = length
                while (remaining > 0) {
                    val read = fis.read(buffer, 0, minOf(buffer.size, remaining.toInt()))
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    remaining -= read
                }
                out.flush()
                fis.close()
            } finally {
                try {
                    pfd?.close()
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            // La TV cerró la conexión o el archivo ya no está: no pasa nada.
        } finally {
            try {
                input?.close()
            } catch (e: Exception) {
            }
            try {
                socket.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun writeSimpleResponse(out: BufferedOutputStream, code: Int, message: String) {
        val body = message.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code\r\nContent-Type: text/plain\r\n" +
            "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(body)
        out.flush()
    }
}
