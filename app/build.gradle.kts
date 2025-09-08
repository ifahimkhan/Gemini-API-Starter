plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.fahim.C006_geminiapistarter"
    compileSdk = 35
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.fahim.C006_geminiapistarter"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val apiKey: String = project.findProperty("GEMINI_API_KEY") as String? ?: ""
        buildConfigField("String", "API_KEY", "\"${apiKey}\"")
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
    implementation(libs.room.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor(libs.room.compiler)
}