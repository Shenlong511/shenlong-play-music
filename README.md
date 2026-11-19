# Shenlong Play Music

Reproductor de música y video local para Android. La música y los
videos están en el teléfono y se reproducen sin internet, sin ningún
permiso de red. (La transmisión Cast a la TV de la 1.4 se quitó en
la 1.5 porque hacía que la app se cerrara al abrir.)

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
  controles (aleatorio, anterior, play/pausa, stop, siguiente y
  repetir) y se esconden solos a los 3 segundos. En vertical hay un botón de giro (junto a la lupa y en la
  tarjeta "Sonando ahora") para pasar a horizontal, y en horizontal el
  mismo giro vuelve a vertical.
- Todos los botones de reproducción son iconos de tamaño fijo (48 dp,
  icono de 24 dp dentro), sin letras, en una sola fila de 6 iconos:
  aleatorio, anterior, play/pausa, siguiente, repetir y **STOP**. El
  botón de stop detiene por completo la reproducción (no es pausa): se
  para todo, se libera la cola y se quita la notificación. También hay
  stop en el video (vertical y horizontal), en la pantalla completa de
  la visualización y en la notificación expandida.
- Videos: sección "Videos" en el menú de ajustes, **agrupados por
  carpetas como la música** (se pueden ocultar carpetas y volver a
  mostrarlas una por una en "Carpetas ocultas", las mismas de la
  música), con miniatura. Al abrir un video en vertical se ve arriba
  en tamaño normal estilo YouTube, con la lista debajo; solo al girar
  a horizontal (o pulsar pantalla completa) se expande a toda la
  pantalla. Formatos comunes: MP4, MKV, WebM, 3GP. El video usa el
  mismo reproductor y servicio que la música: al cerrar la pantalla,
  minimizar la app o apagar la pantalla, el audio sigue sonando en
  segundo plano, con los controles en la notificación y en el bloqueo.
  Al volver a la app, la tarjeta "Video en segundo plano" lo retoma
  con "Ver video" y se vuelve a ver la imagen (al volver de la
  ventanita flotante también se reengancha el video). Al cambiar de
  música a video (o al revés) con el botón rápido, la reproducción
  anterior se DETIENE por completo (no solo se pausa).
- Transmisión a TV (Cast, v1.4): botón Cast en el encabezado y en las
  pantallas de video. Busca Chromecast, TV con Google TV o TV box en
  la misma WiFi y manda lo que suena (canción o video) a la TV; el
  teléfono se queda en pausa. Como la TV no puede leer los archivos
  del teléfono, la app levanta un servidor HTTP mínimo solo en la red
  WiFi de casa y le pasa a la TV la dirección local del archivo; nada
  se sube a internet y el servidor se apaga al terminar la transmisión.
  El teléfono y la TV tienen que estar en la misma WiFi. Si el
  teléfono no tiene Google Play Services, el botón Cast no muestra
  dispositivos y todo lo demás sigue funcionando igual.
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
- La música y los videos se reproducen sin internet (archivos locales
  del teléfono). Permisos nuevos de la 1.4 solo para Cast: INTERNET,
  ACCESS_NETWORK_STATE y ACCESS_WIFI_STATE (permisos normales, no se
  piden al usuario; solo sirven para descubrir la TV en la WiFi de casa
  y mandarle el archivo por la red local, sin subir nada a internet).
- Stack: Kotlin 1.9.24, AGP 8.4.2, Gradle 8.6, Compose 1.5.14,
  Media3 (ExoPlayer) 1.3.1, Google Cast 21.4.0.
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

## Novedades de la 1.5 (Cast quitado, lo demás de la 1.4 se queda)

- Cast quitado por completo: sin botón Cast, sin CastManager, sin
  CastOptionsProvider, sin permisos de red (INTERNET,
  ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE) y sin las librerías
  mediarouter ni play-services-cast-framework. La 1.4 compilaba en
  verde pero se cerraba al abrir ("continúa fallando") justo después
  de la pantalla del logo, y la 1.3 sin Cast abría bien: lo único
  nuevo de la 1.4 que corría al arrancar era Cast.
- Se conservan todas las demás novedades de la 1.4: botón de stop,
  videos por carpetas con ocultar/mostrar, video que se sigue viendo
  al volver de la ventanita PiP, detener por completo al cambiar de
  música a video y viceversa, video en vertical estilo YouTube y
  aleatorio/repetir en el visualizador horizontal.

## Novedades de la 1.4 (stop, videos por carpetas, video en vertical, Cast)
- Botón de STOP (icono de cuadrado): detiene por completo la música y el
  video, en vertical y en horizontal (tarjeta "Sonando ahora", video en
  vertical, video a pantalla completa, visualización a pantalla completa,
  tarjeta "Video en segundo plano" y notificación expandida). No es
  pausa: se para todo, se libera la cola y se quita la notificación.
- Videos por carpetas: en vez de una sola lista general, los videos se
  agrupan por carpeta como la música, con las mismas carpetas ocultas
  (ocultar una carpeta la quita de música y de videos).
- Video en vertical estilo YouTube: al abrir un video en vertical se ve
  arriba en tamaño normal (16:9) con la lista debajo; solo en horizontal
  (o con el botón de pantalla completa) se expande a toda la pantalla.
- Al volver de la ventanita flotante (PiP) a la app, el video se vuelve
  a ver: se reengancha la superficie al salir de PiP y cuando se crea
  una superficie nueva. Al cambiar de música a video (o al revés) con el
  botón rápido, la reproducción anterior se detiene por completo (stop,
  no pausa).
- En la visualización a pantalla completa en horizontal ya están también
  los botones de aleatorio y repetir, junto a anterior, play/pausa,
  stop y siguiente (6 iconos de 48 dp, con el estado activo visible).
- Transmisión a TV (Google Cast / Chromecast): botón Cast en el
  encabezado y en las pantallas de video. Permisos nuevos: INTERNET,
  ACCESS_NETWORK_STATE y ACCESS_WIFI_STATE (solo para la red local).
