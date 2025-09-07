plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android") // Needed for Kotlin support
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

        // Gemini API key from gradle.properties
        buildConfigField(
            "String",
            "API_KEY",
            "\"${project.property("GEMINI_API_KEY")}\""
        )

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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Core & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.common)

    // Gemini AI SDK
    implementation(libs.generativeai)

    // RecyclerView for chat
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

fun <BaseAppModuleExtension> kotlinOptions(function: () -> Unit): BaseAppModuleExtension {

}
