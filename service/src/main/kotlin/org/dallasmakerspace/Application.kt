package org.dallasmakerspace

import io.ktor.server.application.*
import org.dallasmakerspace.plugins.configureHTTP
import org.dallasmakerspace.plugins.configureMonitoring
import org.dallasmakerspace.plugins.configureRouting
import org.dallasmakerspace.plugins.configureSerialization

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
  configureSerialization()
  configureMonitoring()
  configureHTTP()
  configureRouting()
}
