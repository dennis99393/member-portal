package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject

class LoginHandler @Inject constructor() : IRouteHandler {
  override suspend fun handle(call: ApplicationCall) {
    call.respondRedirect(RouteFactory.Paths.INDEX.path, permanent = false)
  }
}
