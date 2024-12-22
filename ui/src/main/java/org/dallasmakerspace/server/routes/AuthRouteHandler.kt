package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.HttpException
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.plugins.UserSession

abstract class AuthRouteHandler(
    loggerFactory: LoggerFactory,
    private val userInfoProvider: UserInfoProvider
) : IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  protected lateinit var userInfo: Map<String, Any>
  protected var session: UserSession? = null
  protected var isInfra = false

  override suspend fun handle(call: ApplicationCall) {
    session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {

      try {
        userInfo = userInfoProvider.getUserInfo(accessToken).toMutableMap()

        // Only allow Infra team members to access the site for now
        val user = userInfo["preferred_username"] as String
        isInfra = (userInfo["groups"] as List<*>?)?.contains("/Infrastructure") ?: false
        call.sessions.set(session?.copy(userId = user))
      } catch (e: HttpException) {
        call.sessions.clear<UserSession>()
        log.error("Failed to get user info: HttpException", e)
        redirectToLogin(call)
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        log.error("Failed to get user info", e)
        throw e
      }
    } else {
      redirectToLogin(call)
    }
  }

  private suspend fun redirectToLogin(call: ApplicationCall) {
    val currentUri = call.request.uri
    val encodedUri =
        withContext(Dispatchers.IO) {
          URLEncoder.encode(currentUri, StandardCharsets.UTF_8.toString())
        }
    call.respondRedirect(
        "${RouteFactory.Paths.LOGIN.path}?redirectUrl=$encodedUri",
        permanent = false,
    )
  }
}
