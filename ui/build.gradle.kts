description = "Web UI for Member Profiles"

val ktorVersion = "2.3.5"
val daggerVersion = "2.48"
val kotlinxVersion = "1.6.0"
val logbackVersion = "1.4.14"
val kotlinTestUnit = "1.9.23"
val seleniumVersion = "4.19.1"

repositories { mavenCentral() }

dependencies {
  implementation("ch.qos.logback", "logback-classic", logbackVersion)
  implementation("com.google.dagger", "dagger", daggerVersion)
  ksp("com.google.dagger", "dagger-compiler", daggerVersion)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("io.ktor", "ktor-client-auth", ktorVersion)
  implementation("io.ktor", "ktor-client-cio", ktorVersion)
  implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)
  implementation("io.ktor", "ktor-client-core", ktorVersion)
  implementation("io.ktor", "ktor-client-logging", ktorVersion)
  implementation("io.ktor", "ktor-serialization-gson-jvm", ktorVersion)
  implementation("io.ktor", "ktor-serialization-jackson", ktorVersion)
  implementation("io.ktor", "ktor-server-auth", ktorVersion)
  implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
  implementation("io.ktor:ktor-server-compression:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor", "ktor-server-core", ktorVersion)
  implementation("io.ktor:ktor-server-call-id:$ktorVersion")
  implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
  implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
  implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktorVersion")
  implementation("io.ktor", "ktor-server-netty", ktorVersion)
  implementation("io.ktor", "ktor-server-status-pages", ktorVersion)
  implementation("io.ktor", "ktor-server-thymeleaf-jvm", ktorVersion)
  implementation("io.ktor:ktor-server-webjars:$ktorVersion")
  implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinxVersion)
  // Elasticsearch logging
  implementation("co.elastic.clients:elasticsearch-java:8.14.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
  implementation("com.agido:logback-elasticsearch-appender:3.0.11")

  testImplementation("io.ktor", "ktor-client-mock", ktorVersion)
  testImplementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
  testImplementation("io.ktor", "ktor-server-tests", ktorVersion)
  testImplementation("org.jetbrains.kotlin", "kotlin-test-junit", kotlinTestUnit)
  testImplementation("org.seleniumhq.selenium", "selenium-java", seleniumVersion)
}

plugins {
  kotlin("jvm") version "1.9.23"
  kotlin("plugin.serialization") version "1.9.23"
  id("com.google.devtools.ksp") version "1.9.23-1.0.20"
  id("io.ktor.plugin") version "2.3.5"
  id("org.jetbrains.kotlinx.kover") version "0.7.6"
  id("com.ncorti.ktfmt.gradle") version "0.18.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

version = "0.0.1"

application {
  mainClass.set("org.dallasmakerspace.server.MemberProfileUiApplicationKt")
  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin { jvmToolchain(20) }
