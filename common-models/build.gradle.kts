plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
}

kotlin {
  jvmToolchain(21)
}
