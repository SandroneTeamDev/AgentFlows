plugins {
    id("com.android.application") version "9.5.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.5.1" apply false
    kotlin("multiplatform") version "2.4.21" apply false
    kotlin("android") version "2.4.21" apply false
    kotlin("jvm") version "2.4.21" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}

val releaseVersion = Regex("\\*\\*v([0-9]+\\.[0-9]+\\.[0-9]+-rc[0-9]+-alpha[0-9]+)")
    .find(rootProject.file("README.md").readText())
    ?.groupValues?.get(1)
    ?: error("README.md must declare the current release version")

allprojects {
    group = "sandrone.agentflows"
    version = releaseVersion
}
