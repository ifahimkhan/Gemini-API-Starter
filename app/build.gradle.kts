import java.io.File
import java.io.FileInputStream
import java.util.Properties

// Function to read properties from local.properties file
fun gradleLocalProperties(projectRootDir: File, providers: org.gradle.api.provider.ProviderFactory): Properties {
    val props = Properties()
    val localPropertiesFile = File(projectRootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { props.load(it) }
    }
    return props
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fahim.geminiapistarter"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.fahim.geminiapistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Use the function here
        val props = gradleLocalProperties(rootDir, providers)
        val geminiKey = props.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.common)
    implementation(libs.generativeai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // AndroidX UI
    implementation("androidx.appcompat:appcompat:1.7.0")
// AppCompat Activity, DayNight theme [13]
    implementation("androidx.core:core-ktx:1.13.1")
// Core APIs; use 1.17.0+ if calling WindowCompat.enableEdgeToEdge [17]
    implementation("androidx.recyclerview:recyclerview:1.3.2")
// RecyclerView [2]
    implementation("com.google.android.material:material:1.13.0")
// Material 3 widgets [8][20]
// Guava Futures for Java callbacks (Futures, FutureCallback, ListenableFuture)
    implementation("com.google.guava:guava:31.1-android")
// Android-friendly Guava futures [22][7]
// Optional: concurrency utils if using androidx concurrent futures
// implementation "androidx.concurrent:concurrent-futures:1.2.0" // only if needed [23]
}
