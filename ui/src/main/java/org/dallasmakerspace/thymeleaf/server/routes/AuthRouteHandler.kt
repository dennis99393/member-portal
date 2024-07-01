package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.HttpException
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession

abstract class AuthRouteHandler(private val userInfoProvider: UserInfoProvider) : IRouteHandler {
  protected lateinit var userInfo: Map<String, Any>

  override suspend fun handle(call: ApplicationCall) {
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {

      try {
        userInfo = userInfoProvider.getUserInfo(accessToken).toMutableMap()
      } catch (e: HttpException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
        throw e
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
        throw e
      }
    } else {
      val currentUri = call.request.uri
      val encodedUri =
          withContext(Dispatchers.IO) {
            URLEncoder.encode(currentUri, StandardCharsets.UTF_8.toString())
          }
      call.respondRedirect(
          "${RouteFactory.Paths.LOGIN.path}?redirectUrl=$encodedUri", permanent = false)
    }
  }
}
