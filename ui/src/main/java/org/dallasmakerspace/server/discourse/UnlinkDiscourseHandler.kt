package org.dallasmakerspace.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.routes.AuthenticatedHandler

class UnlinkDiscourseHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  override suspend fun handleAuthenticated(call: ApplicationCall) {

    val username =
        userInfo["preferred_username"] as? String
            ?: throw AuthException("No username found in user info")
    memberService.unlinkDiscourseAccount(username, session.sessionId)
    call.respondRedirect("/profile/@$username", permanent = false)
  }
}
