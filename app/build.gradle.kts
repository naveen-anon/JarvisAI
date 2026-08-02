plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

tasks.register("generateColorResources") {
    val colorsFile = file("src/main/res/values/colors.xml")
    colorsFile.parentFile.mkdirs()
    colorsFile.writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="hud_bg">#050A0F</color>
    <color name="hud_cyan">#00D4FF</color>
    <color name="hud_cyan_dim">#0A6E85</color>
    <color name="hud_amber">#FFB300</color>
    <color name="hud_white">#E8F9FF</color>
    <color name="hud_text_dim">#5C8A94</color>
    <color name="hud_cyan_faint">#1A2E36</color>
</resources>
    """.trimIndent())
}

tasks.getByName("preBuild").dependsOn("generateColorResources")

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jarvis.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${System.getenv("OPENWEATHER_API_KEY") ?: ""}\"")
        val _jarvisLocalProps = java.util.Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        buildConfigField("String", "GROQ_API_KEY", "\"${_jarvisLocalProps.getProperty("GROQ_API_KEY", System.getenv("GROQ_API_KEY") ?: "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")

    // Networking for Claude API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Porcupine wake-word engine (offline). Get free access key from console.picovoice.ai
    implementation("ai.picovoice:porcupine-android:3.0.3")

    // Phase 4 — Vision: camera capture + on-device ML for OCR / object / face detection.
    // All three ML Kit APIs below run fully on-device, no network call, no cloud quota.
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:face-detection:16.1.6")
}
