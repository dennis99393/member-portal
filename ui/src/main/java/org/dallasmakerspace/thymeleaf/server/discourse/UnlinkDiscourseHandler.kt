package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.routes.AuthRouteHandler
import javax.inject.Inject
import kotlin.random.Random

class UnlinkDiscourseHandler
@Inject
constructor(
  private val memberService: MemberService,
  private val userInfoProvider: UserInfoProvider
) : AuthRouteHandler(userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    val username =
        userInfo["preferred_username"] as? String
            ?: throw AuthException("No username found in user info")
    memberService.unlinkDiscourseAccount(username)
    call.respondRedirect("/profile/@$username", permanent = false)
  }
}
