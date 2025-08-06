pluginManagement {
    repositories {
        // Plugin downloads (Android Gradle Plugin, Kotlin, Compose Compiler, etc.)
        gradlePluginPortal()
        google()
        mavenCentral()
        jcenter()
    }
}

dependencyResolutionManagement {
    // Don’t allow modules to add their own repos


    // All project dependencies (including ExoPlayer) must come from here:
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MediaPlayerApp"
include(":app")
