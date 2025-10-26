package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.plugins.AuthException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    // Process history - limit to last 5 events and convert to Central Time
    if (requestedGroup.history.isNotEmpty()) {
      val centralZone = ZoneId.of("America/Chicago")
      val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")

      val processedHistory =
          requestedGroup.history.take(5).map { event ->
            val centralTime = event.eventTimestamp.atZone(centralZone)

            // Find member details from the group members list
            val memberMember = requestedGroup.members.find { it.username == event.memberUsername }

            // Handle special service accounts for actor
            val actorData =
                when {
                  // Special case: svc_makermanager3 in "Voting Members" group should show "self"
                  requestedGroupName.equals("Voting Members", ignoreCase = true) &&
                      event.actorUsername.equals("svc_makermanager3", ignoreCase = true) ->
                      mapOf(
                          "displayName" to "self",
                          "link" to "#",
                          "isExternal" to false,
                      )
                  event.actorUsername.toLowerCasePreservingASCIIRules() == "svc_modile" ->
                      mapOf(
                          "displayName" to "DMS Learn",
                          "link" to "https://learn.dallasmakerspace.org",
                          "isExternal" to true,
                      )
                  event.actorUsername.toLowerCasePreservingASCIIRules() == "svc_makermanager3" ->
                      mapOf(
                          "displayName" to "DMS Calendar",
                          "link" to "https://calendar.dallasmakerspace.org",
                          "isExternal" to true,
                      )
                  else -> {
                    val actorMember =
                        requestedGroup.members.find { it.username == event.actorUsername }
                    mapOf(
                        "displayName" to (actorMember?.displayName ?: event.actorUsername),
                        "link" to "/profile/@${event.actorUsername}",
                        "isExternal" to false,
                    )
                  }
                }

            mutableMapOf<String, Any?>(
                "actorDisplayName" to actorData["displayName"],
                "actorLink" to actorData["link"],
                "actorIsExternal" to actorData["isExternal"],
                "memberUsername" to event.memberUsername,
                "memberDisplayName" to (memberMember?.displayName ?: event.memberUsername),
                "formattedTimestamp" to centralTime.format(dateTimeFormatter),
                "timestamp" to event.eventTimestamp.toString(),
            )
          }
      jsonMap["history"] = processedHistory
    }

    requestedGroup.description?.let { jsonMap["description"] = it }
    call.respond(ThymeleafContent("group", jsonMap))
  }
}
