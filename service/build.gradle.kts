
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val daggerVersion: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("io.ktor.plugin") version "2.3.10"
    id("com.google.devtools.ksp") version "1.9.23-1.0.20"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    id("com.ncorti.ktfmt.gradle") version "0.18.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

group = "org.dallasmakerspace"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.dagger:dagger:$daggerVersion")
    ksp ("com.google.dagger:dagger-compiler:$daggerVersion")
    // Ktor client packages used for making HTTP requests
    implementation("io.ktor:ktor-client-core:2.0.0")
    implementation("io.ktor:ktor-client-cio:2.0.0")
    implementation("io.ktor:ktor-client-json:2.0.0")
    implementation("io.ktor:ktor-client-logging:2.0.0")
    // Ktor server packages
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.h2database:h2:2.1.214")
    implementation("io.ktor:ktor-serialization-gson-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-swagger-jvm")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("com.ucasoft.ktor:ktor-simple-cache:0.+")
    implementation("com.ucasoft.ktor:ktor-simple-memory-cache:0.+")
    implementation("com.unboundid:unboundid-ldapsdk:6.0.7") // LDAP Integration
    implementation("io.ktor:ktor-server-caching-headers-jvm")
    implementation("io.ktor:ktor-server-compression-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.0")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
