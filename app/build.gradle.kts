plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.fahim.geminiapistarter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fahim.geminiapistarter"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // BuildConfig API Key
        buildConfigField("String", "API_KEY", "\"AIzaSyDYweE-EBZXP7OggfgOO_qICKZvsNhC4Lg\"")
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
    // Room database dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Generative AI / Gemini client
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Coroutines for any async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
