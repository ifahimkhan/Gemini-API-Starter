pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // JetBrains Space repo for Gemini/Generative AI client

    }
}

rootProject.name = "MAD-Assignment-1"
include(":app")
