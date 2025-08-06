plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}



android {
    namespace = "com.cosmin23.mediaplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cosmin23.mediaplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        // Activează Compose
        compose = true
    }

//    composeOptions {
//        // Versiunea compatibilă a compilatorului Compose
//        kotlinCompilerExtensionVersion = "1.5.4"
//    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
repositories {
    google()
    mavenCentral()
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.1")
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-ui:1.0.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")


    // BOM pentru Compose – sincronizează versiunile tuturor componentelor
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.0")

    // UI Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ViewModel & LiveData în Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.1")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Debug tooling (Preview)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testare
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.6")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

