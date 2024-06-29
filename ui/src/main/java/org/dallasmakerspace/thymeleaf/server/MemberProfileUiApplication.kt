package org.dallasmakerspace.thymeleaf.server

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent
import org.dallasmakerspace.thymeleaf.server.plugins.configureRouting
import org.dallasmakerspace.thymeleaf.server.plugins.configureStatusPages
import org.dallasmakerspace.thymeleaf.server.plugins.configureTemplating

fun main() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  val portStr = appConfig.requireStringProperty("ktor.deployment.port")
  embeddedServer(Netty, port = portStr.toInt(), host = "0.0.0.0") {
        configureTemplating()
        configureRouting()
        configureStatusPages()
      }
      .start(wait = true)
}
