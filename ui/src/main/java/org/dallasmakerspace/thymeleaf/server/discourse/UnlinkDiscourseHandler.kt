package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.routes.AuthRouteHandler

class UnlinkDiscourseHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberService: MemberService,
    userInfoProvider: UserInfoProvider
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    val username =
        userInfo["preferred_username"] as? String
            ?: throw AuthException("No username found in user info")
    memberService.unlinkDiscourseAccount(username, session?.sessionId)
    call.respondRedirect("/profile/@$username", permanent = false)
  }
}
