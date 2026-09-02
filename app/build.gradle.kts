import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load API keys from local.properties (preferred for local builds) or environment variables (CI)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun apiKey(name: String): String =
    localProps.getProperty(name)?.trim().orEmpty()
        .ifEmpty { System.getenv(name)?.trim().orEmpty() }

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 34

    setProperty("archivesBaseName", "Jarvis")

    defaultConfig {
        applicationId = "com.jarvis.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Gemini / OpenWeather / Groq / Picovoice / ElevenLabs (human TTS)
        // Set in local.properties OR export as env vars / GitHub Actions secrets
        buildConfigField("String", "GEMINI_API_KEY", "\"${apiKey("GEMINI_API_KEY")}\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${apiKey("OPENWEATHER_API_KEY")}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${apiKey("GROQ_API_KEY")}\"")
        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"${apiKey("PICOVOICE_ACCESS_KEY")}\"")
        buildConfigField("String", "ELEVENLABS_API_KEY", "\"${apiKey("ELEVENLABS_API_KEY")}\"")
        // Optional: Voices → copy ID from https://elevenlabs.io/app/voice-library
        buildConfigField("String", "ELEVENLABS_VOICE_ID", "\"${apiKey("ELEVENLABS_VOICE_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
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
        compose = true
    }
    composeOptions {
        // Compatible with Kotlin 1.9.24 (root build.gradle.kts)
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("ai.picovoice:porcupine-android:3.0.3")

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Jetpack Compose (used by com.jarvis.ai armor suit UI)
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
