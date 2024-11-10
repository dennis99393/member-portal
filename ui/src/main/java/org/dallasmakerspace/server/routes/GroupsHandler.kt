package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import kotlin.collections.set
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.plugins.AuthException

class GroupsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberService: MemberService,
    userInfoProvider: UserInfoProvider
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    // Get username requested from path /profile/@{preferred_username}
    val requestedGroupSlug =
        call.parameters["group_slug"] ?: throw AuthException("No group slug found in url path")
    val requestedGroupName = DMSGroup.getNameFromSlug(requestedGroupSlug)
    val requestedGroup = memberService.getGroup(requestedGroupSlug, session?.sessionId)

    log.debug("Group: {}", requestedGroup)
    val jsonMap: MutableMap<String, Any> =
        mutableMapOf(
            "name" to requestedGroupName,
            "slug" to requestedGroup.slug,
        )
    if (requestedGroup.members.isNotEmpty()) {
      jsonMap["members"] = requestedGroup.members
      jsonMap["memberlist_incomplete"] = requestedGroup.membersListIncomplete
    }
    requestedGroup.description?.let { jsonMap["description"] = it }
    call.respond(ThymeleafContent("group", jsonMap))
  }
}
