package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.plugins.UserSession

/**
 * Base class for handlers that require authentication.
 * Assumes the route is wrapped in authenticate("auth_session") in Routing.kt.
 * Provides common authenticated request context (session, userInfo, authorization flags).
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
    // Get session (guaranteed to exist by auth_session authentication)
    session = call.sessions.get<UserSession>()
        ?: throw AuthException("No session found")

    // Fetch user info from OAuth provider
    userInfo = userInfoProvider.getUserInfo(session.accessToken!!)

    // Extract authorization flags from groups
    val userGroups = (userInfo["groups"] as? List<*>) ?: emptyList<String>()
    isInfra = userGroups.contains("/Infrastructure")
    isBoard = userGroups.contains("/Board")
    isOfficer = userGroups.contains("/Officers")

    // Delegate to subclass
    handleAuthenticated(call)
  }

  /**
   * Handle the authenticated request. Session and userInfo are already populated.
   */
  protected abstract suspend fun handleAuthenticated(call: ApplicationCall)
}
