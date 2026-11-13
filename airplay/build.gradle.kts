import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// AirPlay receiver for CarHub. Core code from jqssun/android-airplay-server v0.0.36 (GPL-3.0,
// based on UxPlay); native libraries are that release's own builds (see src/main/jniLibs).
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "io.github.jqssun.airplay"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-transformer:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
