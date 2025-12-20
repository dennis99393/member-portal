// Root build file for member-portal monorepo
// Individual module configurations are in service/build.gradle.kts and ui/build.gradle.kts

plugins {
    base
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.2" apply false
    id("io.ktor.plugin") version "3.3.0" apply false
    id("org.jetbrains.kotlinx.kover") version "0.7.6" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

// Load local properties if they exist (gitignored for machine-specific settings)
val localPropertiesFile = file("gradle.properties.local")
if (localPropertiesFile.exists()) {
    val localProperties = java.util.Properties()
    localProperties.load(localPropertiesFile.inputStream())
    localProperties.forEach { key, value ->
        if (key is String && value is String) {
            extra.set(key, value)
        }
    }
}

allprojects {
    group = "org.dallasmakerspace"
    version = "0.0.1"
}
