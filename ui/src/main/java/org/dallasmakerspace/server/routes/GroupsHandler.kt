package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.plugins.AuthException
import javax.inject.Inject

class GroupsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberService: MemberService,
    userInfoProvider: UserInfoProvider,
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {

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
      // Process members to add proper avatar URLs
      val processedMembers =
          requestedGroup.members.map { member ->
            val avatarUrl =
                member.avatarUrl
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { url ->
                      when {
                        url.startsWith("//") -> "https:$url"
                        else -> "https://talk.dallasmakerspace.org$url"
                      }.replace("{size}", "144")
                    } ?: ""

            // Create a map with processed avatar URL
            mutableMapOf<String, Any?>(
                "username" to member.username,
                "displayName" to member.displayName,
                "avatarUrl" to avatarUrl,
                "discourseUsername" to member.discourseUsername,
                "discourseAvatarUrl" to member.discourseAvatarUrl,
                "discordUserId" to member.discordUserId,
            )
          }

      jsonMap["members"] = processedMembers
      jsonMap["memberlist_incomplete"] = requestedGroup.membersListIncomplete
    }

    // Process administrators
    if (requestedGroup.administrators.isNotEmpty()) {
      val processedAdmins =
          requestedGroup.administrators.map { dn ->
            val name = DMSGroup.parseCNFromDN(dn)
            val isGroup = DMSGroup.isGroupDN(dn)
            mutableMapOf<String, Any?>(
                "name" to name,
                "isGroup" to isGroup,
                "link" to
                    if (isGroup) "/groups/${DMSGroup.getSlugFromName(name)}" else "/profile/@$name",
            )
          }
      jsonMap["administrators"] = processedAdmins
    }

    requestedGroup.description?.let { jsonMap["description"] = it }
    call.respond(ThymeleafContent("group", jsonMap))
  }
}
