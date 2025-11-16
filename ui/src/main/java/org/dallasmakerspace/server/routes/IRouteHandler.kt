package org.dallasmakerspace.server.routes

import io.ktor.server.application.*

interface IRouteHandler {
  suspend fun handleBase(call: ApplicationCall) = handle(call)

  suspend fun handle(call: ApplicationCall)
}
