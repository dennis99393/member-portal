package org.dallasmakerspace.server.voterregistration

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.routes.AuthRouteHandler

class RegisterVotingHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {

    // Get username requested from path /profile/@{preferred_username}
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")

    val currentUsername = userInfo["preferred_username"] as String?

    if (requestedUsername != currentUsername &&
        !VoterRegistrationManager.IS_VOTER_REGISTRATION_TEST_MODE_ENABLED) {
      throw AuthException("You can only register yourself for voting")
    }

    memberService.registerVoting(session?.sessionId, requestedUsername)

    // Set success flag in session
    session?.isVoterRegistrationSuccess = true
    call.sessions.set(session)

    call.respondRedirect("/profile/@$requestedUsername", permanent = false)
  }
}
