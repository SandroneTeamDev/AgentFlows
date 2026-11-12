import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "sandrone.agentflows.desktop.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "AgentFlows"
            packageVersion = project.version.toString().substringBefore('-')
            description = "A local-first personal Super-agent workspace"
            vendor = "SandroneTeamDev"
            licenseFile.set(rootProject.file("LICENSE"))
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.net.http", "java.sql", "jdk.unsupported")
            linux {
                packageName = "agentflows"
                appCategory = "Development"
                debMaintainer = "SandroneTeamDev"
            }
            macOS {
                bundleID = "sandrone.agentflows.desktop"
                packageVersion = "1." + project.version.toString().substringBefore('-').substringAfter('.')
            }
            windows {
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "f6c3cff9-c72d-4b9a-a9c4-6c2ac6daaaf7"
            }
        }
    }
}
