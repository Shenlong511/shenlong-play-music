package com.fran.shenlongplaymusic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.io.ByteArrayOutputStream
import java.util.Random

/**
 * Arte de paisajes dibujado en código, sin fotos ni internet.
 *
 * Android no permite una visualización animada en vivo en la barra de
 * notificaciones ni en la pantalla de bloqueo: ahí solo se muestra una
 * carátula fija junto a los controles. Así que cada canción lleva como
 * carátula uno de estos 20 paisajes distintos, dibujados con Canvas
 * (cielo en degradado, sol o luna, montañas en capas, lago con reflejo
 * y siluetas de árboles). El paisaje se ve en la notificación y en la
 * pantalla de bloqueo, y cambia con cada canción.
 *
 * El dibujo es determinista por escena (siempre sale el mismo paisaje
 * para el mismo número) y los 20 bitmaps/PNG se guardan en caché en
 * memoria, así que no se vuelve a dibujar nada al cambiar de canción.
 */
object LandscapeArt {

    const val COUNT = 20

    private const val SUN_NONE = 0
    private const val SUN_SUN = 1
    private const val SUN_MOON = 2

    private data class Scene(
        val name: String,
        val skyTop: String,
        val skyBottom: String,
        val sunKind: Int = SUN_SUN,
        val sunColor: String = "#FFF3B0",
        val sunX: Float = 0.72f,
        val sunY: Float = 0.22f,
        val stars: Boolean = false,
        val aurora: Boolean = false,
        val farColor: String,
        val nearColor: String,
        val lake: Boolean = true,
        val lakeTop: String = "#7FB3D5",
        val lakeBottom: String = "#2E5A7A",
        val groundColor: String = "#3E7A44",
        val treeColor: String = "#1E3A24",
        val trees: Boolean = false,
        val flowers: Boolean = false,
        val snow: Boolean = false,
        val volcano: Boolean = false,
        val waterfall: Boolean = false,
        val blossom: Boolean = false,
        val city: Boolean = false
    )

    // 20 escenas, cada una con paleta propia para que se distingan.
    private val scenes = listOf(
        Scene("Atardecer en el mar", "#FF7E3A", "#FFD166",
            sunColor = "#FFF3B0", sunX = 0.5f, sunY = 0.42f,
            farColor = "#8A4A6B", nearColor = "#5B2A52",
            lakeTop = "#FF9E5E", lakeBottom = "#5B2A52"),
        Scene("Amanecer en montañas", "#FFD9A0", "#7FB3D5",
            sunColor = "#FFF8DC", sunX = 0.3f, sunY = 0.2f,
            farColor = "#7A9BB5", nearColor = "#4A6B4F",
            lakeTop = "#A8D0E8", lakeBottom = "#4E7A9B"),
        Scene("Noche estrellada", "#0B1026", "#274060",
            sunKind = SUN_MOON, sunColor = "#F2F2E8", sunX = 0.72f, sunY = 0.18f,
            stars = true, farColor = "#1E2A44", nearColor = "#16213A",
            lakeTop = "#2A4A6B", lakeBottom = "#101E33"),
        Scene("Desierto", "#FFE8B3", "#FF9E57",
            sunColor = "#FFF8DC", sunX = 0.68f, sunY = 0.18f,
            farColor = "#D98E4A", nearColor = "#B96A2E", lake = false,
            groundColor = "#E8B96A", treeColor = "#7A4A21", trees = true),
        Scene("Montañas nevadas", "#BFE3F2", "#EAF6FB",
            sunColor = "#FFFFFF", sunX = 0.28f, sunY = 0.16f,
            farColor = "#8FA8BF", nearColor = "#5E7A94", snow = true,
            lakeTop = "#CFE8F2", lakeBottom = "#7AA8BF"),
        Scene("Bosque verde", "#CDEEB0", "#8FCB9B",
            sunColor = "#FFF8DC", sunX = 0.7f, sunY = 0.18f,
            farColor = "#6FA36B", nearColor = "#3E7A44", lake = false,
            groundColor = "#2F5D33", treeColor = "#1E3A24", trees = true),
        Scene("Playa tropical", "#7FD4F2", "#FFF3B0",
            sunColor = "#FFF8DC", sunX = 0.72f, sunY = 0.2f,
            farColor = "#7FB3A0", nearColor = "#4E9B8A",
            lakeTop = "#4ED0C8", lakeBottom = "#1E7A8A",
            treeColor = "#1E4A2E", trees = true),
        Scene("Cascada", "#A8D8EA", "#E8F6F3",
            sunColor = "#FFF8DC", sunX = 0.3f, sunY = 0.18f,
            farColor = "#6B8E7A", nearColor = "#3E5E4B", waterfall = true,
            lakeTop = "#8FD0E0", lakeBottom = "#3E7A8A"),
        Scene("Campo de flores", "#BFE6FF", "#FFF0F5",
            sunColor = "#FFF8DC", sunX = 0.68f, sunY = 0.18f,
            farColor = "#8FBF8A", nearColor = "#5E9B58", lake = false,
            groundColor = "#7AB648", flowers = true),
        Scene("Aurora boreal", "#0A1F33", "#1E4D5B",
            sunKind = SUN_MOON, sunColor = "#E8F2F2", sunX = 0.24f, sunY = 0.16f,
            stars = true, aurora = true, farColor = "#22384A", nearColor = "#14262F",
            lakeTop = "#1E5B5B", lakeBottom = "#0A2A33"),
        Scene("Volcán", "#2B1B2E", "#FF7B3D",
            sunColor = "#FF6B3D", sunX = 0.5f, sunY = 0.3f,
            farColor = "#4A2A3A", nearColor = "#2E1F2A", lake = false,
            groundColor = "#241A20", volcano = true),
        Scene("Lago de montaña", "#9FD0F2", "#EAF7FF",
            sunColor = "#FFF8DC", sunX = 0.72f, sunY = 0.18f,
            farColor = "#7A9CC0", nearColor = "#4E6E8E",
            lakeTop = "#A8D8F2", lakeBottom = "#3E6E9B"),
        Scene("Otoño", "#FFD98A", "#FF9E6B",
            sunColor = "#FFF3B0", sunX = 0.3f, sunY = 0.2f,
            farColor = "#A86B4A", nearColor = "#7A4A2E", lake = false,
            groundColor = "#B07A3E", treeColor = "#C05A2E", trees = true),
        Scene("Cerezos en flor", "#FFD6E8", "#FFF6F9",
            sunColor = "#FFF8DC", sunX = 0.68f, sunY = 0.18f,
            farColor = "#B08AA8", nearColor = "#7A5A7A", lake = false,
            groundColor = "#8FBF7A", blossom = true),
        Scene("Ciudad al atardecer", "#FF9E57", "#5B2A6B",
            sunColor = "#FFD166", sunX = 0.5f, sunY = 0.38f,
            farColor = "#6B3A6B", nearColor = "#3A2A4A", lake = false,
            groundColor = "#241A33", city = true),
        Scene("Pradera", "#A8E0FF", "#FFF9C4",
            sunColor = "#FFF8DC", sunX = 0.7f, sunY = 0.18f,
            farColor = "#8FBE7A", nearColor = "#5E9B4E", lake = false,
            groundColor = "#7AB648", flowers = true),
        Scene("Selva", "#B8E6B0", "#5FA36B",
            sunColor = "#FFF8DC", sunX = 0.32f, sunY = 0.16f,
            farColor = "#4E8A58", nearColor = "#2E5E3A", lake = false,
            groundColor = "#24512E", treeColor = "#122E18", trees = true),
        Scene("Glaciar", "#CDEFFF", "#FFFFFF",
            sunColor = "#FFFFFF", sunX = 0.7f, sunY = 0.18f,
            farColor = "#A8C8E0", nearColor = "#7AA8C8", snow = true,
            lakeTop = "#BFE3F2", lakeBottom = "#6B9BB5"),
        Scene("Cañón rojo", "#FFE0B3", "#FF8A5C",
            sunColor = "#FFF3B0", sunX = 0.28f, sunY = 0.2f,
            farColor = "#C05A3A", nearColor = "#8A3A26", lake = false,
            groundColor = "#A85A3A"),
        Scene("Luna llena sobre el lago", "#101830", "#3A4A7A",
            sunKind = SUN_MOON, sunColor = "#F5F5DC", sunX = 0.5f, sunY = 0.2f,
            stars = true, farColor = "#2A3550", nearColor = "#1A2438",
            lakeTop = "#2E4A7A", lakeBottom = "#0E1A33")
    )

    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private val bytesCache = mutableMapOf<Int, ByteArray>()

    /** Nombre del paisaje, para depurar o mostrarlo en la app. */
    fun name(index: Int): String = scenes[((index % COUNT) + COUNT) % COUNT].name

    /**
     * Paisaje que toca a cada canción: estable por canción (depende de su
     * id de MediaStore), así que la misma canción siempre muestra el
     * mismo paisaje y las canciones vecinas muestran paisajes distintos.
     */
    fun indexForSong(song: Song): Int {
        val idx = ((song.id % COUNT) + COUNT) % COUNT
        return idx.toInt()
    }

    fun bitmapForSong(song: Song): Bitmap = bitmap(indexForSong(song))

    fun artworkBytesForSong(song: Song): ByteArray = artworkBytes(indexForSong(song))

    /**
     * Paisaje que toca a cada video (v1.8): igual que las canciones,
     * estable por video según su id de MediaStore, así que el mismo
     * video siempre muestra el mismo paisaje en la notificación y en
     * la pantalla de bloqueo.
     */
    fun indexForVideo(video: VideoItem): Int {
        val idx = ((video.id % COUNT) + COUNT) % COUNT
        return idx.toInt()
    }

    fun bitmapForVideo(video: VideoItem): Bitmap = bitmap(indexForVideo(video))

    fun artworkBytesForVideo(video: VideoItem): ByteArray = artworkBytes(indexForVideo(video))

    fun bitmap(index: Int, size: Int = 512): Bitmap {
        val i = ((index % COUNT) + COUNT) % COUNT
        synchronized(bitmapCache) {
            bitmapCache[i]?.let { return it }
            val bmp = draw(i, size)
            bitmapCache[i] = bmp
            return bmp
        }
    }

    /** PNG del paisaje, para la carátula de la MediaSession (bloqueo). */
    fun artworkBytes(index: Int): ByteArray {
        val i = ((index % COUNT) + COUNT) % COUNT
        synchronized(bytesCache) {
            bytesCache[i]?.let { return it }
            val out = ByteArrayOutputStream()
            bitmap(i).compress(Bitmap.CompressFormat.PNG, 90, out)
            val bytes = out.toByteArray()
            bytesCache[i] = bytes
            return bytes
        }
    }

    private fun draw(index: Int, size: Int): Bitmap {
        val scene = scenes[index]
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val f = size.toFloat()
        val horizon = f * 0.66f

        // Cielo en degradado.
        paint.shader = LinearGradient(
            0f, 0f, 0f, horizon,
            Color.parseColor(scene.skyTop), Color.parseColor(scene.skyBottom),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, f, horizon, paint)
        paint.shader = null

        // Estrellas (semilla fija: siempre las mismas en cada escena).
        if (scene.stars) {
            val rnd = Random(1000L + index)
            repeat(90) {
                paint.color = Color.argb(140 + rnd.nextInt(115), 255, 255, 255)
                val r = 1f + rnd.nextFloat() * 2.2f
                canvas.drawCircle(rnd.nextFloat() * f, rnd.nextFloat() * f * 0.55f, r, paint)
            }
        }

        // Aurora boreal: bandas verdes/cian translúcidas en el cielo.
        if (scene.aurora) {
            val auroraColors = intArrayOf(
                Color.argb(70, 80, 255, 160),
                Color.argb(55, 80, 200, 255),
                Color.argb(60, 160, 255, 120)
            )
            repeat(3) { band ->
                paint.color = auroraColors[band]
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = f * 0.09f
                val path = Path()
                val baseY = f * (0.12f + band * 0.09f)
                path.moveTo(0f, baseY)
                var x = 0f
                while (x <= f) {
                    path.lineTo(x, baseY + Math.sin((x / f) * Math.PI * 2 + band).toFloat() * f * 0.05f)
                    x += f / 16f
                }
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // Sol o luna, con brillo alrededor.
        if (scene.sunKind != SUN_NONE) {
            val cx = scene.sunX * f
            val cy = scene.sunY * f
            val r = f * 0.09f
            val sunColor = Color.parseColor(scene.sunColor)
            paint.shader = RadialGradient(
                cx, cy, r * 2.2f,
                Color.argb(90, Color.red(sunColor), Color.green(sunColor), Color.blue(sunColor)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r * 2.2f, paint)
            paint.shader = null
            paint.color = sunColor
            canvas.drawCircle(cx, cy, r, paint)
            if (scene.sunKind == SUN_MOON) {
                // Cráteres de la luna.
                paint.color = Color.argb(70, 120, 120, 130)
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.2f, r * 0.22f, paint)
                canvas.drawCircle(cx + r * 0.35f, cy + r * 0.3f, r * 0.16f, paint)
                canvas.drawCircle(cx + r * 0.1f, cy - r * 0.45f, r * 0.12f, paint)
            }
        }

        // Montañas lejanas y cercanas.
        drawRidge(canvas, paint, f, horizon * 0.82f, f * 0.16f, 3000L + index, Color.parseColor(scene.farColor))
        drawRidge(canvas, paint, f, horizon * 0.96f, f * 0.13f, 4000L + index, Color.parseColor(scene.nearColor))

        if (scene.volcano) {
            // Cono del volcán con lava, en el centro del horizonte.
            paint.color = Color.parseColor("#2E1F2A")
            val path = Path()
            path.moveTo(f * 0.32f, horizon)
            path.lineTo(f * 0.5f, horizon - f * 0.22f)
            path.lineTo(f * 0.68f, horizon)
            path.close()
            canvas.drawPath(path, paint)
            paint.color = Color.parseColor("#FF6B1A")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = f * 0.02f
            canvas.drawLine(f * 0.5f, horizon - f * 0.22f, f * 0.5f, horizon - f * 0.08f, paint)
            canvas.drawLine(f * 0.47f, horizon - f * 0.14f, f * 0.53f, horizon - f * 0.06f, paint)
            paint.style = Paint.Style.FILL
            // Humo.
            paint.color = Color.argb(80, 180, 180, 180)
            canvas.drawCircle(f * 0.5f, horizon - f * 0.26f, f * 0.05f, paint)
            canvas.drawCircle(f * 0.54f, horizon - f * 0.32f, f * 0.04f, paint)
        }

        if (scene.lake) {
            // Lago con degradado y reflejo del sol/luna.
            paint.shader = LinearGradient(
                0f, horizon, 0f, f,
                Color.parseColor(scene.lakeTop), Color.parseColor(scene.lakeBottom),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, horizon, f, f, paint)
            paint.shader = null
            if (scene.sunKind != SUN_NONE) {
                val sunColor = Color.parseColor(scene.sunColor)
                paint.color = Color.argb(70, Color.red(sunColor), Color.green(sunColor), Color.blue(sunColor))
                canvas.drawRect(scene.sunX * f - f * 0.05f, horizon, scene.sunX * f + f * 0.05f, f, paint)
                paint.color = Color.argb(90, 255, 255, 255)
                val rnd = Random(5000L + index)
                repeat(8) {
                    val y = horizon + rnd.nextFloat() * (f - horizon)
                    val w = f * (0.04f + rnd.nextFloat() * 0.08f)
                    canvas.drawRect(scene.sunX * f - w / 2, y, scene.sunX * f + w / 2, y + 3f, paint)
                }
            }
            if (scene.waterfall) {
                // Cascada: banda clara que cae de la montaña al lago.
                paint.color = Color.argb(220, 230, 250, 255)
                val x0 = f * 0.58f
                canvas.drawRect(x0, horizon - f * 0.18f, x0 + f * 0.06f, horizon + f * 0.1f, paint)
                paint.color = Color.argb(200, 255, 255, 255)
                canvas.drawRect(x0 + f * 0.015f, horizon - f * 0.18f, x0 + f * 0.03f, horizon + f * 0.1f, paint)
                // Espuma al pie de la cascada.
                canvas.drawCircle(x0 + f * 0.03f, horizon + f * 0.1f, f * 0.05f, paint)
            }
        } else {
            // Tierra: suelo, ciudad, árboles, flores o nieve.
            paint.color = Color.parseColor(scene.groundColor)
            canvas.drawRect(0f, horizon, f, f, paint)

            if (scene.city) {
                // Silueta de edificios con ventanitas encendidas.
                val rnd = Random(6000L + index)
                var x = 0f
                while (x < f) {
                    val w = f * (0.07f + rnd.nextFloat() * 0.08f)
                    val h = f * (0.08f + rnd.nextFloat() * 0.16f)
                    paint.color = Color.parseColor("#1A1226")
                    canvas.drawRect(x, horizon + f * 0.28f - h, x + w, horizon + f * 0.28f, paint)
                    paint.color = Color.argb(220, 255, 220, 120)
                    var wy = horizon + f * 0.28f - h + f * 0.02f
                    while (wy < horizon + f * 0.26f) {
                        if (rnd.nextFloat() < 0.5f) {
                            canvas.drawRect(x + w * 0.2f, wy, x + w * 0.4f, wy + f * 0.015f, paint)
                        }
                        wy += f * 0.04f
                    }
                    x += w + f * 0.015f
                }
            }

            if (scene.trees) {
                val rnd = Random(7000L + index)
                paint.color = Color.parseColor(scene.treeColor)
                repeat(9) {
                    val x = rnd.nextFloat() * f
                    val h = f * (0.07f + rnd.nextFloat() * 0.09f)
                    val w = f * (0.045f + rnd.nextFloat() * 0.02f)
                    val baseY = f * (0.86f + rnd.nextFloat() * 0.12f)
                    val path = Path()
                    path.moveTo(x - w, baseY)
                    path.lineTo(x, baseY - h)
                    path.lineTo(x + w, baseY)
                    path.close()
                    canvas.drawPath(path, paint)
                    canvas.drawRect(x - w * 0.12f, baseY, x + w * 0.12f, baseY + f * 0.03f, paint)
                }
            }

            if (scene.flowers) {
                val rnd = Random(8000L + index)
                val petalColors = intArrayOf(
                    Color.parseColor("#FF5A8A"), Color.parseColor("#FFD166"),
                    Color.WHITE, Color.parseColor("#FF8A3D"), Color.parseColor("#B07FE8")
                )
                repeat(70) {
                    paint.color = petalColors[rnd.nextInt(petalColors.size)]
                    canvas.drawCircle(
                        rnd.nextFloat() * f,
                        horizon + rnd.nextFloat() * (f - horizon),
                        2f + rnd.nextFloat() * 3f, paint
                    )
                }
            }

            if (scene.blossom) {
                // Pétalos rosas flotando y en el suelo.
                val rnd = Random(9000L + index)
                repeat(50) {
                    paint.color = Color.argb(200, 255, 170, 200)
                    canvas.drawCircle(
                        rnd.nextFloat() * f, rnd.nextFloat() * f,
                        3f + rnd.nextFloat() * 4f, paint
                    )
                }
            }

            if (scene.snow) {
                val rnd = Random(10000L + index)
                paint.color = Color.WHITE
                repeat(60) {
                    canvas.drawCircle(
                        rnd.nextFloat() * f,
                        horizon + rnd.nextFloat() * (f - horizon),
                        2f + rnd.nextFloat() * 3f, paint
                    )
                }
            }
        }
        return bmp
    }

    /** Cresta de montaña: línea quebrada determinista hasta abajo. */
    private fun drawRidge(
        canvas: Canvas, paint: Paint, size: Float,
        baseY: Float, amplitude: Float, seed: Long, color: Int
    ) {
        val rnd = Random(seed)
        val path = Path()
        path.moveTo(0f, baseY)
        var x = 0f
        while (x <= size) {
            path.lineTo(x, baseY - rnd.nextFloat() * amplitude)
            x += size / 16f
        }
        path.lineTo(size, size)
        path.lineTo(0f, size)
        path.close()
        paint.color = color
        canvas.drawPath(path, paint)
    }
}
