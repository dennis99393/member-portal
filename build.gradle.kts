// Root build file for member-portal monorepo
// Individual module configurations are in service/build.gradle.kts and ui/build.gradle.kts

plugins {
    base
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
