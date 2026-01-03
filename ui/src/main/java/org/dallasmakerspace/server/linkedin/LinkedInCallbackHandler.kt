package org.dallasmakerspace.server.linkedin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.plugins.UserSession
import org.dallasmakerspace.server.routes.IRouteHandler

/**
 * Handles the OAuth2 callback from LinkedIn after a user has authorized account linking.
 * - Validates the state parameter for CSRF protection
 * - Exchanges the authorization code for an access token
 * - Fetches user info from LinkedIn API
 * - Links the LinkedIn account to the DMS member profile
 */
class LinkedInCallbackHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val linkedInOAuthProvider: LinkedInOAuthProvider,
    private val memberService: MemberService,
    private val userInfoProvider: UserInfoProvider,
    private val linkedInStateCache: LinkedInStateCache
) : IRouteHandler {

  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    // Get code and state from query parameters
    val code = call.request.queryParameters["code"]
    val state = call.request.queryParameters["state"]
    val error = call.request.queryParameters["error"]

    // Handle error response from LinkedIn
    if (error != null) {
      val errorDescription = call.request.queryParameters["error_description"] ?: error
      log.error("LinkedIn authorization failed: $errorDescription")
      throw LinkedInException("LinkedIn authorization failed: $errorDescription")
    }

    // Validate required parameters
    if (code == null) {
      throw LinkedInException("Missing authorization code")
    }
    if (state == null) {
      throw LinkedInException("Missing state parameter")
    }

    // Get session and validate user
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken ?: throw LinkedInException("No session found")

    val username =
        try {
          val jsonMap = userInfoProvider.getUserInfo(accessToken)
          jsonMap["preferred_username"] as? String
              ?: throw LinkedInException("No username in user info")
        } catch (e: AuthException) {
          call.sessions.clear<UserSession>()
          log.error("Failed to get user info", e)
          throw LinkedInException("Failed to get user info")
        }

    // Validate state parameter for CSRF protection
    val originalState =
        linkedInStateCache.getState(username)
            ?: throw LinkedInException("No state found in cache - session may have expired")

    if (state != originalState) {
      throw LinkedInException("State mismatch - possible CSRF attack")
    }

    // Delete the state from cache
    linkedInStateCache.deleteState(username)

    // Exchange code for access token
    val linkedInAccessToken = linkedInOAuthProvider.exchangeCodeForToken(code)

    // Fetch LinkedIn user info
    val linkedInUserInfo = linkedInOAuthProvider.fetchUserInfo(linkedInAccessToken)

    log.info(
        "Linking LinkedIn account ${linkedInUserInfo.name} (${linkedInUserInfo.userId}) to DMS user $username")

    // Link the LinkedIn account
    // Use the name as the display username since LinkedIn doesn't expose vanity URLs via OIDC
    memberService.linkLinkedInAccount(username, linkedInUserInfo.name, session.sessionId)

    // Set success flag in session
    session.isLinkedInLinkSuccess = true
    call.sessions.set(session)

    // Redirect back to profile page
    call.respondRedirect("/profile/@$username", permanent = false)
  }
}
