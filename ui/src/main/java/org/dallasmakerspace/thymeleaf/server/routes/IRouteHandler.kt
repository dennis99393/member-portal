package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*

interface IRouteHandler {
  suspend fun handle(call: ApplicationCall)
}
