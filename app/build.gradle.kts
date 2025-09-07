    plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.C147Tanush.assignment1"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.C147Tanush.assignment1"
        minSdk = 26
        targetSdk = 36
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
    testImplementation(libs.junit)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.room:room-runtime:2.7.2")
    annotationProcessor("androidx.room:room-compiler:2.7.2")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}