package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession

class ProfileHandler @Inject constructor(private val userInfoProvider: UserInfoProvider) :
    IRouteHandler {
  override suspend fun handle(call: ApplicationCall) {
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {
      val jsonMap: Map<String, Any>?

      try {
        jsonMap = userInfoProvider.getUserInfo(accessToken)
        call.respond(ThymeleafContent("profile", jsonMap))
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
        call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false)
      }
    }
  }
}
