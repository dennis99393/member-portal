package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.datetime.toJavaLocalDateTime
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.getNameFromSlug
import org.dallasmakerspace.server.models.getSlugFromName
import org.dallasmakerspace.server.models.isGroupDN
import org.dallasmakerspace.server.models.parseCNFromDN
import org.dallasmakerspace.server.plugins.AuthException

class GroupsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {

    // Get group slug requested from path /groups/{group_slug}
    val requestedGroupSlug =
        call.parameters["group_slug"] ?: throw AuthException("No group slug found in url path")
    val requestedGroupName = getNameFromSlug(requestedGroupSlug)
    val requestedGroup = memberService.getGroup(requestedGroupSlug, session.sessionId)

    log.debug("Group: {}", requestedGroup)
    val jsonMap: MutableMap<String, Any> =
        mutableMapOf(
            "name" to requestedGroupName,
            "slug" to requestedGroupSlug,
        )
    if (requestedGroup.members.isNotEmpty()) {
      // Create a map of username to most recent timestamp from history
      val memberTimestamps =
          requestedGroup.history
              .groupBy { it.memberUsername }
              .mapValues { (_, events) -> events.maxOf { it.eventTimestamp } }

      // Process members to add proper avatar URLs
      val processedMembers =
          requestedGroup.members
              // Sort by most recent addition first
              .sortedByDescending { member ->
                memberTimestamps[member.username]
                    ?: kotlinx.datetime.LocalDateTime(1970, 1, 1, 0, 0, 0)
              }
              .map { member ->
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
          requestedGroup.administrators
              .filter { it.contains("Domain Admins").not() }
              .map { dn ->
                val name = parseCNFromDN(dn)
                val isGroup = isGroupDN(dn)
                val slug = getSlugFromName(name)
                mutableMapOf<String, Any?>(
                    "name" to name,
                    "isGroup" to isGroup,
                    "slug" to slug,
                    "link" to if (isGroup) "/groups/$slug" else "/profile/@$name",
                )
              }
      jsonMap["administrators"] = processedAdmins
    }

    // Process history - limit to last 5 events and convert to Central Time
    if (requestedGroup.history.isNotEmpty()) {
      val centralZone = ZoneId.of("America/Chicago")
      val today = LocalDate.now(centralZone)

      val processedHistory =
          requestedGroup.history.take(5).map { event ->
            val centralTime = event.eventTimestamp.toJavaLocalDateTime().atZone(centralZone)
            val eventDate = centralTime.toLocalDate()
            val daysAgo = ChronoUnit.DAYS.between(eventDate, today)

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
                  event.actorUsername.toLowerCasePreservingASCIIRules() == "svc_moodle" ->
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

            // Format timestamp as relative time
            val formattedTimestamp =
                when {
                  daysAgo == 0L -> "Today"
                  daysAgo == 1L -> "Yesterday"
                  daysAgo < 7L -> "$daysAgo days ago"
                  daysAgo < 30L -> "${daysAgo / 7} week${if (daysAgo / 7 > 1) "s" else ""} ago"
                  else -> centralTime.format(DateTimeFormatter.ofPattern("MMM d"))
                }

            mutableMapOf<String, Any?>(
                "actorDisplayName" to actorData["displayName"],
                "actorLink" to actorData["link"],
                "actorIsExternal" to actorData["isExternal"],
                "memberUsername" to event.memberUsername,
                "memberDisplayName" to (memberMember?.displayName ?: event.memberUsername),
                "formattedTimestamp" to formattedTimestamp,
                "timestamp" to event.eventTimestamp.toString(),
            )
          }
      jsonMap["history"] = processedHistory
    }

    requestedGroup.description?.let { jsonMap["description"] = it }
    call.respond(ThymeleafContent("group", jsonMap))
  }
}
