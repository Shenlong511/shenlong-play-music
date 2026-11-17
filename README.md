# Shenlong Play Music

Reproductor de música local para Android, sin internet.

## Qué hace

- Lee las canciones del teléfono (MediaStore): MP3, AAC, M4A, FLAC, WAV,
  OGG, OPUS, MIDI, AMR.
- Lista de canciones con búsqueda (lupa vectorial de 48 dp que se expande),
  y reproducción por carpetas (se pueden ocultar carpetas y volver a
  mostrarlas una por una en "Carpetas ocultas").
- Listas de reproducción: crear listas con nombre, agregar canciones
  (botón + de cada canción), quitar canciones y borrar listas. Las listas
  y las carpetas ocultas se guardan en el teléfono (DataStore).
- Botón de ajustes (engranaje) en el encabezado con menú desplegable:
  Todas las canciones, Carpetas, Listas de reproducción, Color de
  tarjetas, Visualización y Ecualizador.
- Color de tarjetas: color fijo (9 colores) + deslizador de transparencia
  0-100%, guardado en el teléfono (DataStore).
- Visualización con 4 estilos (Barras neón, Onda, Círculo y Espejo),
  elegibles en Ajustes > Visualización y guardados en el teléfono
  (DataStore). Al girar el teléfono a horizontal, la visualización ocupa
  toda la pantalla; al pulsar cualquier lugar aparecen/desaparecen los
  controles (anterior, play/pausa, siguiente) y se esconden solos a los
  3 segundos. En vertical hay un botón de giro (junto a la lupa y en la
  tarjeta "Sonando ahora") para pasar a horizontal, y en horizontal el
  mismo giro vuelve a vertical.
- Todos los botones de reproducción son iconos de tamaño fijo (48 dp,
  icono de 24 dp dentro), sin letras: anterior, play/pausa, siguiente,
  aleatorio y repetir.
- Aleatorio, repetir lista, repetir una canción, siguiente/anterior,
  play/pausa y barra de progreso.
- Ecualizador de 5 bandas con presets: Normal, Rock, Pop, Jazz, Clásica,
  Graves fuertes y **Altavoces** (baja graves y sube medios/agudos para
  que el teléfono no se distorsione). También refuerzo de bajos y
  encendido/apagado. Los ajustes se guardan en el teléfono (DataStore).
- Reproducción en segundo plano (servicio en primer plano + MediaSession):
  suena con la pantalla apagada y los controles aparecen en la pantalla
  de bloqueo y en la notificación. Cada canción muestra como carátula un
  paisaje distinto (20 paisajes dibujados en código, sin fotos ni
  internet: atardecer, amanecer, noche estrellada, desierto, nieve,
  bosque, playa, cascada, flores, aurora, volcán, lago, otoño, cerezos,
  ciudad, pradera, selva, glaciar, cañón y luna llena), que cambia al
  pasar de canción. En la notificación y en el bloqueo no se puede poner
  la visualización en vivo (Android solo admite carátula fija), por eso
  se usan estos paisajes.
- Widget de pantalla principal con anterior, play/pausa y siguiente.
- Fondo claro con acentos neón (azul, rosa, rojo, verde, naranja,
  amarillo, morado). Botones de icono de 48 dp que no se deforman en
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
