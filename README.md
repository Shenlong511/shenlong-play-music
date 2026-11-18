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
  Todas las canciones, Carpetas, Listas de reproducción, Videos, Color de
  tarjetas, Color de fondo, Visualización y Ecualizador. También tiene el
  interruptor "Permitir giro de pantalla": apagado, la app queda fija en
  vertical.
- Color de tarjetas y Color de fondo: color fijo (17 colores, en una fila
  con desplazamiento para que ninguno se corte) + deslizador de
  transparencia 0-100%, guardados en el teléfono (DataStore).
- Visualización con 4 estilos (Barras neón, Onda, Círculo y Espejo),
  elegibles en Ajustes > Visualización y guardados en el teléfono
  (DataStore). Al girar el teléfono a horizontal, la visualización ocupa
  toda la pantalla; al pulsar cualquier lugar aparecen/desaparecen los
  controles (anterior, play/pausa, siguiente) y se esconden solos a los
  3 segundos. En vertical hay un botón de giro (junto a la lupa y en la
  tarjeta "Sonando ahora") para pasar a horizontal, y en horizontal el
  mismo giro vuelve a vertical.
- Todos los botones de reproducción son iconos de tamaño fijo (48 dp,
  icono de 24 dp dentro), sin letras, en una sola fila de 5 iconos:
  aleatorio, anterior, play/pausa, siguiente y repetir.
- Videos: sección "Videos" en el menú de ajustes con la lista de videos
  del teléfono (MP4, MKV, WebM, 3GP y demás formatos comunes, con
  miniatura) y reproducción a pantalla completa con ExoPlayer. El video
  usa el mismo reproductor y servicio que la música: al cerrar la
  pantalla, minimizar la app o apagar la pantalla, el audio sigue
  sonando en segundo plano, con los controles en la notificación y en el
  bloqueo. Al volver a la app, la tarjeta "Video en segundo plano" lo
  retoma con "Ver video".
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

## Novedades de la 1.3 (Picture-in-Picture y cambio rápido)
- Picture-in-Picture: con un video a pantalla completa, al pulsar el botón
  de inicio el video sigue en una ventanita flotante por encima de otras
  apps (se mueve y se cambia de tamaño arrastrando sus esquinas, como
  permite Android). La ventanita lleva su botón play/pausa y al pulsarla
  se vuelve a pantalla completa. Al poner música o cerrar el video, la app
  sale de la ventanita. Requiere Android 8.0 o superior; en versiones
  anteriores el video sigue solo con el audio en segundo plano.
- Cambio rápido música / video: botón con icono en el encabezado (junto a
  la lupa y el engranaje) que con un solo toque cambia entre la lista de
  música y la de videos, sin entrar a Ajustes. Al cambiar se pausa lo que
  esté sonando.
