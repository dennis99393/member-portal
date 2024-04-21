package org.dallasmakerspace.thymeleaf.server

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.dallasmakerspace.thymeleaf.server.plugins.configureRouting
import org.dallasmakerspace.thymeleaf.server.plugins.configureStatusPages
import org.dallasmakerspace.thymeleaf.server.plugins.configureTemplating

fun main() {
  val config = ApplicationConfig(null)
  val portStr = config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8000"
  embeddedServer(Netty, port = portStr.toInt(), host = "0.0.0.0") {
        configureTemplating()
        configureRouting()
        configureStatusPages()
      }
      .start(wait = true)
}
