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

        // ✅ Hardcoded API Key (replace with your own key if needed)
        buildConfigField("String", "API_KEY", "\"AIzaSyCDYqM75r7UBNH42ZDkANkzrt588LcWhnc\"")

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

    // Gemini libraries
    implementation(libs.common)
    implementation(libs.generativeai)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
