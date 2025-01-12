package org.dallasmakerspace.server.voterregistration

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.routes.AuthRouteHandler

class UnregisterVotingHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    // Get username requested from path /profile/@{preferred_username}
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")

    val currentUsername = userInfo["preferred_username"] as String?

    if (requestedUsername != currentUsername &&
        !VoterRegistrationManager.IS_VOTER_REGISTRATION_TEST_MODE_ENABLED) {
      throw AuthException("You can only cancel your own registration")
    }

    memberService.unregisterVoting(session?.sessionId, requestedUsername)

    call.respondRedirect("/profile/@$requestedUsername", permanent = false)
  }
}
