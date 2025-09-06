buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0") // Use latest stable
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    }
}

// No plugins {} block here; module-level applies the plugin
