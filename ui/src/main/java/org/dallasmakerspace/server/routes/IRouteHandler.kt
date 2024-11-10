package org.dallasmakerspace.server.routes

import io.ktor.server.application.*

interface IRouteHandler {
  suspend fun handle(call: ApplicationCall)
}
