pluginManagement {
    repositories {
        // Plugin downloads (Android Gradle Plugin, Kotlin, Compose Compiler, etc.)
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    // All project dependencies (including Media3/ExoPlayer) must come from here:
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MediaPlayerApp"
include(":app")
