package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.HttpException
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.plugins.UserSession

/**
 * Base class for handlers that require authentication. Assumes the route is wrapped in
 * authenticate("auth_session") in Routing.kt. Provides common authenticated request context
 * (session, userInfo, authorization flags).
 */
abstract class AuthenticatedHandler(
    protected val loggerFactory: LoggerFactory,
    private val userInfoProvider: UserInfoProvider
) : IRouteHandler {

  protected lateinit var session: UserSession
  protected lateinit var userInfo: Map<String, Any>
  protected var isInfra = false
  protected var isBoard = false
  protected var isOfficer = false

  final override suspend fun handle(call: ApplicationCall) {
    val log = loggerFactory.create(javaClass)

    // Get session (guaranteed to exist by auth_session authentication)
    session = call.sessions.get<UserSession>() ?: throw AuthException("No session found")

    // Fetch user info from OAuth provider
    try {
      userInfo = userInfoProvider.getUserInfo(session.accessToken!!)
    } catch (e: HttpException) {
      if (e.message?.contains("401") == true) {
        // Token expired or invalid - clear session and redirect to login
        log.warn("Access token expired or invalid, redirecting to login")
        call.sessions.clear<UserSession>()
        val currentUri = call.request.uri
        val encodedUri = java.net.URLEncoder.encode(currentUri, "UTF-8")
        call.respondRedirect("${RouteFactory.Paths.LOGIN.path}?redirectUrl=$encodedUri")
        return
      }
      throw e
    }

    // Extract authorization flags from groups
    val userGroups = (userInfo["groups"] as? List<*>) ?: emptyList<String>()
    isInfra = userGroups.contains("/Infrastructure")
    isBoard = userGroups.contains("/Board")
    isOfficer = userGroups.contains("/Officers")

    // Delegate to subclass
    handleAuthenticated(call)
  }

  /** Handle the authenticated request. Session and userInfo are already populated. */
  protected abstract suspend fun handleAuthenticated(call: ApplicationCall)
}
