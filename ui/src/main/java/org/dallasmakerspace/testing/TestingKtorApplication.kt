package org.dallasmakerspace.testing

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.dallasmakerspace.testing.plugins.configureRouting

fun main() {
  embeddedServer(Netty, port = 8000, host = "0.0.0.0") { configureRouting() }.start(wait = true)
}
