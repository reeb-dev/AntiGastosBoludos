import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// El plugin de Firebase/Google Services requiere un `google-services.json` válido
// dentro de `app/`. Si todavía no lo configuraste, no aplicamos el plugin
// (la app sigue compilando, pero Analytics/IA de Firebase quedan limitados).
val googleServicesJson = projectDir.resolve("google-services.json")
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

fun localProp(name: String): String {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    return (props.getProperty(name) ?: System.getenv(name) ?: "").trim()
}

val tenorApiKey: String = localProp("TENOR_API_KEY")
val giphyApiKey: String = localProp("GIPHY_API_KEY")
val klipyApiKey: String = localProp("KLIPY_API_KEY")
/**
 * API key de Google Gemini que viaja embebida en el APK. Si está seteada,
 * los usuarios NO necesitan poner la suya y todo el costo lo paga el dueño
 * del proyecto (free tier ~1500 req/día/key). Cargala en `local.properties`:
 *
 *     GEMINI_API_KEY=AIza...
 */
val geminiApiKey: String = localProp("GEMINI_API_KEY")
val donationCbu: String = localProp("DONATION_CBU")
val donationHolder: String = localProp("DONATION_HOLDER")
val donationAlias: String = localProp("DONATION_ALIAS")
val releaseStoreFile: String = localProp("RELEASE_STORE_FILE")
val releaseStorePassword: String = localProp("RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String = localProp("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String = localProp("RELEASE_KEY_PASSWORD")

android {
    namespace = "com.antigastos.boludos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.antigastos.boludos"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DONATION_CBU", "\"$donationCbu\"")
        buildConfigField("String", "DONATION_HOLDER", "\"$donationHolder\"")
        buildConfigField("String", "DONATION_ALIAS", "\"$donationAlias\"")
    }

    signingConfigs {
        if (releaseStoreFile.isNotBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "TENOR_API_KEY", "\"$tenorApiKey\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
            buildConfigField("String", "KLIPY_API_KEY", "\"$klipyApiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        }
        release {
            isMinifyEnabled = false
            // Nunca embeber GEMINI en release: usar Firebase AI (google-services.json).
            buildConfigField("String", "TENOR_API_KEY", "\"$tenorApiKey\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
            buildConfigField("String", "KLIPY_API_KEY", "\"$klipyApiKey\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            if (releaseStoreFile.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Empaqueta las .so sin compresión y respetando la alineación
            // ELF de origen, requerido para soportar dispositivos con páginas
            // de 16 KB (obligatorio en Google Play desde 01/11/2025).
            useLegacyPackaging = false
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val room = "2.8.4"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase: IA opcional (sin login ni Firestore).
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doFirst {
            if (releaseStoreFile.isBlank()) {
                error(
                    "Firma release no configurada. Definí RELEASE_STORE_FILE (y credenciales) " +
                        "en local.properties antes de generar el AAB de producción.",
                )
            }
        }
    }
}
