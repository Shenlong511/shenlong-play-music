plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma propia (keystore de David): si GitHub Actions pasa la llave por
// variables de entorno, la compilación debug se firma con ella para poder
// actualizar sin desinstalar. Sin esas variables (compilación local),
// se usa la llave debug automática.
// Los nombres no coinciden con las propiedades de SigningConfig:
// dentro de create("release") { ... }, "keyAlias = keyAlias" se
// asignaría a sí mismo y la llave quedaría vacía (eso tumbó la
// ejecución #10 de Biblia SL con 'missing required property "keyPassword"').
val keystoreFilePath = System.getenv("KEYSTORE_FILE")
val keystorePass = System.getenv("KEYSTORE_PASSWORD")
val keyAliasValue = System.getenv("KEY_ALIAS")
val keyPassValue = System.getenv("KEY_PASSWORD")
val hasSigningKey = !keystoreFilePath.isNullOrBlank()
    && !keystorePass.isNullOrBlank()
    && !keyAliasValue.isNullOrBlank()
    && !keyPassValue.isNullOrBlank()

android {
    namespace = "com.fran.shenlongplaymusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fran.shenlongplaymusic"
        minSdk = 24
        targetSdk = 34
        versionCode = 16
        versionName = "2.5"
    }

    signingConfigs {
        if (hasSigningKey) {
            create("release") {
                storeFile = file(keystoreFilePath!!)
                storePassword = keystorePass
                keyAlias = keyAliasValue
                keyPassword = keyPassValue
            }
        }
    }

    buildTypes {
        debug {
            if (hasSigningKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Reproductor local (ExoPlayer / Media3). Sin internet: solo archivos del teléfono.
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    // Para NotificationCompat.MediaStyle (controles en bloqueo/notificación)
    // y el token de la MediaSession.
    implementation("androidx.media:media:1.7.0")
    // v1.5: Cast quitado (la 1.4 se cerraba al abrir por Cast).
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
