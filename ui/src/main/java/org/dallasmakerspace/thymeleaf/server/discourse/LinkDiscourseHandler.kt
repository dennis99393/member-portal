package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler
import org.dallasmakerspace.thymeleaf.server.routes.RouteFactory

class LinkDiscourseHandler
@Inject
constructor(
    private val userInfoProvider: UserInfoProvider,
    private val discourseLinkProvider: DiscourseSSOProvider
) : IRouteHandler {

  override suspend fun handle(call: ApplicationCall) {
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {
      val jsonMap: Map<String, Any>?

      try {
        jsonMap = userInfoProvider.getUserInfo(accessToken).toMutableMap()
        // val username = jsonMap["preferred_username"] as String?
        val discourseSSOConnectUrl = discourseLinkProvider.getDiscourseSSOConnectUrl()
        call.respondRedirect(discourseSSOConnectUrl, permanent = false)
        return
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
      }
    }
    call.respondRedirect(RouteFactory.Paths.INDEX.path, permanent = false)
  }
}
