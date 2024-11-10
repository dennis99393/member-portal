package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import javax.inject.Inject
import org.dallasmakerspace.server.common.SessionIdGenerator
import org.dallasmakerspace.server.plugins.UserSession

class OidcCallbackHandler @Inject constructor() : IRouteHandler {
  private val sessionIdGenerator = SessionIdGenerator(length = 8)

  override suspend fun handle(call: ApplicationCall) {
    val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
    if (principal == null) {
      call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false)
      return
    }
    val session = call.sessions.getOrSet { UserSession() }
    session.accessToken = principal.accessToken
    session.idHint = principal.extraParameters["id_token"]
    session.sessionId = sessionIdGenerator.generate()
    val state = principal.state
    RouteFactory.redirects[state]?.let { redirect ->
      call.respondRedirect(redirect)
      return
    }

    call.respondRedirect("/", permanent = false)
  }
}
