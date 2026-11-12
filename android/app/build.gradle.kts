plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "sandrone.agentflows.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "sandrone.agentflows.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = project.version.toString()
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
