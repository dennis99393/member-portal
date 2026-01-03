package org.dallasmakerspace.server.linkedin

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import kotlin.random.Random
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.routes.AuthenticatedHandler

private const val STATE_RANGE_START = 1000000
private const val STATE_RANGE_END = 9999999

/**
 * Initiates the LinkedIn OAuth2 account linking flow. Generates a random state for CSRF protection
 * and redirects to LinkedIn authorization.
 */
class LinkLinkedInHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val linkedInStateCache: LinkedInStateCache,
    private val linkedInOAuthProvider: LinkedInOAuthProvider
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val username =
        userInfo["preferred_username"] as? String
            ?: throw AuthException("No username found in user info")

    // Generate random state for CSRF protection
    val randomState = Random.nextInt(STATE_RANGE_START, STATE_RANGE_END).toString()
    linkedInStateCache.setState(username, randomState)

    // Redirect to LinkedIn authorization URL
    val linkedInAuthUrl = linkedInOAuthProvider.getLinkedInAuthorizationUrl(randomState)
    call.respondRedirect(linkedInAuthUrl, permanent = false)
  }
}
