plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
    // Firebase / Google Services. `apply false` acá; en el módulo `app` se aplica
    // condicionalmente solo si existe el archivo `google-services.json`.
    id("com.google.gms.google-services") version "4.4.4" apply false
}
