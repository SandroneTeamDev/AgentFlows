plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
    jvm("desktop")

    androidLibrary {
        namespace = "sandrone.agentflows.shared"
        compileSdk = 37
        minSdk = 29
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            api(compose.material3)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("dev.chrisbanes.haze:haze:2.1.0")
            implementation("dev.chrisbanes.haze:haze-blur:2.1.0")
            implementation("dev.chrisbanes.haze:haze-blur-material3:2.1.0")
            implementation("dev.chrisbanes.haze:haze-glass:2.1.0")
            implementation("dev.chrisbanes.haze:haze-glass-material3:2.1.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val androidMain by getting {
            kotlin.srcDir("src/jvmMain/kotlin")
        }
        val desktopMain by getting {
            kotlin.srcDir("src/jvmMain/kotlin")
        }
    }
}
