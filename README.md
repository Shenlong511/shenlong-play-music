# Shenlong Play Music

Reproductor de música local para Android, sin internet.

## Qué hace

- Lee las canciones del teléfono (MediaStore): MP3, AAC, M4A, FLAC, WAV,
  OGG, OPUS, MIDI, AMR.
- Lista de canciones con búsqueda, y reproducción por carpetas.
- Aleatorio, repetir lista, repetir una canción, siguiente/anterior,
  play/pausa y barra de progreso.
- Ecualizador de 5 bandas con presets: Normal, Rock, Pop, Jazz, Clásica,
  Graves fuertes y **Altavoces** (baja graves y sube medios/agudos para
  que el teléfono no se distorsione). También refuerzo de bajos y
  encendido/apagado. Los ajustes se guardan en el teléfono (DataStore).
- Reproducción en segundo plano (servicio en primer plano + MediaSession):
  suena con la pantalla apagada y los controles aparecen en la pantalla
  de bloqueo y en la notificación.
- Widget de pantalla principal con anterior, play/pausa y siguiente.
- Fondo claro con acentos neón (azul, rosa, rojo, verde, naranja,
  amarillo, morado). Botones compactos de 36 dp que no se deforman en
  horizontal ni en vertical, y el encabezado con los controles se esconde
  al bajar sobre la lista de canciones (como en Biblia SL 1.2).

## Compilar la APK en GitHub Actions

1. Sube este proyecto a un repositorio de GitHub (rama `main`).
2. Abre la pestaña **Actions** (Acciones) y espera a que termine
   **Compilar APK (debug)**.
3. Descarga el artifact **shenlong-play-music-debug-apk**, descomprime el
   ZIP y encontrarás `app-debug.apk`.
4. Mándate la APK al teléfono (por ejemplo por Telegram) e instálala.
   Al abrirla, da permiso para leer audio y copia canciones a tu teléfono.

## Notas

- Paquete: `com.fran.shenlongplaymusic`. minSdk 24, targetSdk 34.
- Sin permiso de internet: la música está en el teléfono.
- Stack: Kotlin 1.9.24, AGP 8.4.2, Gradle 8.6, Compose 1.5.14,
  Media3 (ExoPlayer) 1.3.1.
- Como cada APK de prueba tiene una firma distinta, para instalar una
  versión nueva hay que desinstalar primero la anterior.
