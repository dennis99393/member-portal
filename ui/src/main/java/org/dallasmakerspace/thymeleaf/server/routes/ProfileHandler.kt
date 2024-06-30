package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import kotlin.collections.set
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException

class ProfileHandler
@Inject
constructor(
    private val memberService: MemberService,
    private val userInfoProvider: UserInfoProvider
) : AuthRouteHandler(userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    // Get username requested from path /profile/@{preferred_username}
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")
    val requestedMember = memberService.getMember(requestedUsername)
    val jsonMap =
        mutableMapOf(
            "name" to requestedMember.displayName as Any,
            "preferred_username" to requestedMember.username,
            "join_date" to "Apr 2022",
            "membership_duration" to "2.2 years",
        )
    if (requestedMember.discourseUsername.isNullOrEmpty().not()) {
      jsonMap["discourse_username"] = requestedMember.discourseUsername as Any
    }
    val currentUsername = userInfo["preferred_username"] as String?
    jsonMap["is_self"] = (requestedUsername == currentUsername).toString()
    call.respond(ThymeleafContent("profile", jsonMap))
  }
}
