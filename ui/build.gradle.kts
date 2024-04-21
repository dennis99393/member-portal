description = "Web UI for Member Profiles"

val graphQLKotlinVersion = "7.0.1"
val ktorVersion = "2.3.5"
val kotlinxVersion = "1.6.0"
val logbackVersion = "1.4.14"
val kotlinTestUnit = "1.9.10"
val seleniumVersion = "4.19.1"


repositories {
  mavenCentral()
}

dependencies {

  implementation("ch.qos.logback", "logback-classic", logbackVersion)
  implementation("io.ktor", "ktor-client-auth", ktorVersion)
  implementation("io.ktor", "ktor-client-cio", ktorVersion)
  implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)
  implementation("io.ktor", "ktor-client-core", ktorVersion)
  implementation("io.ktor", "ktor-serialization-gson-jvm", ktorVersion)
  implementation("io.ktor", "ktor-serialization-jackson", ktorVersion)
  implementation("io.ktor", "ktor-server-auth", ktorVersion)
  implementation("io.ktor", "ktor-server-core", ktorVersion)
  implementation("io.ktor", "ktor-server-netty", ktorVersion)
  implementation("io.ktor", "ktor-server-status-pages", ktorVersion)
  implementation("io.ktor", "ktor-server-thymeleaf-jvm", ktorVersion)
  implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinxVersion)


  testImplementation("io.ktor", "ktor-client-mock", ktorVersion)
  testImplementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
  testImplementation("io.ktor", "ktor-server-tests", ktorVersion)
  testImplementation("org.jetbrains.kotlin", "kotlin-test-junit", kotlinTestUnit)
  testImplementation("org.seleniumhq.selenium", "selenium-java", seleniumVersion)

}

plugins {
  kotlin("jvm") version "1.9.10"
  kotlin("plugin.serialization") version "1.9.10"
  id("io.ktor.plugin") version "2.3.5"
  id("org.jetbrains.kotlinx.kover") version "0.7.6"
}

application {
  mainClass.set("org.dallasmakerspace.thymeleaf.server.ThymeleafKtorApplicationKt")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

kotlin {
  jvmToolchain(20)
}
