package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession

class OidcCallbackHandler @Inject constructor() : IRouteHandler {
  override suspend fun handle(call: ApplicationCall) {
    val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
    if (principal == null) {
      call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false)
      return
    }
    val session = call.sessions.getOrSet { UserSession() }
    session.accessToken = principal.accessToken
    session.idHint = principal.extraParameters["id_token"]

    call.respondRedirect("/", permanent = false)
  }
}
