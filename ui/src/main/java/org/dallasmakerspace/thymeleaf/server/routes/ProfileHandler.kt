package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.collections.set
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.models.DMSMember
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException

class ProfileHandler
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
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")
    val requestedMember = memberService.getMember(requestedUsername, session?.sessionId)
    val memberSinceString = getMemberSinceString(requestedMember.memberSince)
    val memberDurationString = getMemberDurationString(requestedMember.memberSince)
    val jsonMap =
        mutableMapOf(
            "name" to requestedMember.displayName as Any,
            "preferred_username" to requestedMember.username,
            "member_since" to memberSinceString,
            "membership_duration" to memberDurationString,
            "enabled" to requestedMember.enabled.toString(),
        )
    if (requestedMember.discourseUsername.isNullOrEmpty().not()) {
      jsonMap["discourse_username"] = requestedMember.discourseUsername as Any
    }
    requestedMember.groups
        .firstOrNull { group -> group.name == "Voting Members" }
        ?.apply { jsonMap["is_voting_member"] = "true" }
    val currentUsername = userInfo["preferred_username"] as String?
    jsonMap["is_self"] = (requestedUsername == currentUsername).toString()
    if (jsonMap["is_self"] == "true") {
      requestedMember.personalEmail?.apply { jsonMap["personal_email"] = this as Any }
      requestedMember.badgeNumber?.apply { jsonMap["badge_number"] = this as Any }
      requestedMember.phoneNumber?.apply { jsonMap["phone_number"] = this as Any }
    }
    setToastMessage(call, jsonMap, requestedMember)
    call.respond(ThymeleafContent("profile", jsonMap))
  }

  private fun setToastMessage(
      call: ApplicationCall,
      jsonMap: MutableMap<String, Any>,
      requestedMember: DMSMember
  ) {
    log.info("Setting toast message for ${requestedMember.username} successful discourse link")
    if (session?.isDiscourseLinkSuccess == true) {
      session?.isDiscourseLinkSuccess = false
      call.sessions.set(session)
      jsonMap["toast_message"] =
          "Successfully linked @${requestedMember.discourseUsername} to your profile."
      jsonMap["toast_btn_url"] = "/unlink-discourse"
      jsonMap["toast_btn_label"] = "Unlink"
    }
  }

  /**
   * Calculate the duration of time since the member joined the space.
   *
   * @param memberSince The date the member joined the space. e.g. - 2022-09-10T16:36:20Z
   * @return A human-readable string like - "2 years, 3 months".
   */
  @Suppress("MagicNumber")
  private fun getMemberDurationString(memberSince: Instant?): String {
    if (memberSince == null || Instant.now().isBefore(memberSince)) {
      return "Unknown"
    }
    val now = Instant.now()
    val duration = now.epochSecond - memberSince.epochSecond
    val years = duration / (60 * 60 * 24 * 365)
    val months = (duration % (60 * 60 * 24 * 365)) / (60 * 60 * 24 * 30)
    val days = (duration % (60 * 60 * 24 * 30)) / (60 * 60 * 24)
    val hours = (duration % (60 * 60 * 24)) / (60 * 60)
    // Only show years if greater than 0, if less than 1 month show only days, if less than a day
    // show hours
    return when {
      years > 1 -> "$years years, $months months"
      years > 0 -> "$years year, $months months"
      months > 1 -> "$months months"
      months > 0 -> "$months month"
      days > 1 -> "$days days"
      days > 0 -> "$days day"
      hours > 1 -> "$hours hours"
      else -> "$hours hour"
    }
  }

  /**
   * Format the [DMSMember.memberSince] date into a human-readable string.
   *
   * @param memberSince The date the member joined the space. e.g. - 2022-09-10T16:36:20Z
   * @return A human-readable string like - "Sept 2022".
   */
  private fun getMemberSinceString(memberSince: Instant?): String {
    if (memberSince == null) {
      return "Unknown"
    }
    // Convert Instant to LocalDate CDT
    val localDate = memberSince.atZone(ZoneId.of("America/Chicago")).toLocalDate()
    // Get short version of month with first letter capitalized
    val month = localDate.month.toString().lowercase().replaceFirstChar { it.uppercase() }
    val year = localDate.year.toString()
    return "$month $year"
  }
}
