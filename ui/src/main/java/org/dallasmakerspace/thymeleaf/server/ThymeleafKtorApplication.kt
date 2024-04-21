package org.dallasmakerspace.thymeleaf.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.dallasmakerspace.thymeleaf.server.plugins.configureRouting
import org.dallasmakerspace.thymeleaf.server.plugins.configureStatusPages
import org.dallasmakerspace.thymeleaf.server.plugins.configureTemplating

fun main() {
  embeddedServer(Netty, port = 8000, host = "0.0.0.0") {
        configureTemplating()
        configureRouting()
        configureStatusPages()
      }
      .start(wait = true)
}
