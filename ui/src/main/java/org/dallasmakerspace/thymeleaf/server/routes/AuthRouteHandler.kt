package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession

abstract class AuthRouteHandler(private val userInfoProvider: UserInfoProvider) : IRouteHandler {
  lateinit var userInfo: Map<String, Any>
  override suspend fun handle(call: ApplicationCall) {
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {

      try {
        userInfo = userInfoProvider.getUserInfo(accessToken).toMutableMap()
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
        throw e
      }
    }
    call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false)
  }
}
