package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaInstant
import org.dallasmakerspace.models.DMSMember
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.slug
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.plugins.UserSession
import org.dallasmakerspace.server.voterregistration.VoterRegistrationManager

class ProfileHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
    private val voterRegistrationManager: VoterRegistrationManager,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {

    // Get username requested from path /profile/@{preferred_username}
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")
    val requestedMember = memberService.getMember(requestedUsername, session.sessionId)
    val avatarUrl =
        requestedMember.discourseAvatarUrl
            ?.takeIf { it.isNotEmpty() }
            ?.let { url ->
              when {
                url.startsWith("//") -> "https:$url"
                else -> "https://talk.dallasmakerspace.org$url"
              }.replace("{size}", "144")
            } ?: ""
    val memberSinceString = getMemberSinceString(requestedMember.memberSince?.toJavaInstant())
    val memberDurationString = getMemberDurationString(requestedMember.memberSince?.toJavaInstant())
    val jsonMap =
        mutableMapOf(
            "name" to requestedMember.displayName as Any,
            "preferred_username" to requestedMember.username,
            "member_since" to memberSinceString,
            "membership_duration" to memberDurationString,
            "enabled" to requestedMember.enabled.toString(),
            "avatar_url" to avatarUrl,
        )
    if (requestedMember.groups.isNotEmpty())
        jsonMap["groups"] =
            requestedMember.groups
                .sortedBy { it.name }
                .map { group ->
                  mapOf(
                      "name" to group.name,
                      "slug" to group.slug,
                      "description" to group.description)
                }
    if (requestedMember.discourseUsername.isNullOrEmpty().not()) {
      jsonMap["discourse_username"] = requestedMember.discourseUsername as Any
    }
    if (requestedMember.discordUserId.isNullOrEmpty().not()) {
      jsonMap["discord_userid"] = requestedMember.discordUserId as Any
      requestedMember.discordUsername?.let { jsonMap["discord_username"] = it }
      requestedMember.discordAvatarUrl?.let { jsonMap["discord_avatar_url"] = it }
    }
    if (requestedMember.linkedinUsername.isNullOrEmpty().not()) {
      jsonMap["linkedin_username"] = requestedMember.linkedinUsername as Any
    }
    requestedMember.groups
        .firstOrNull { group -> group.name == voterRegistrationManager.getVotingMembersGroupName() }
        ?.apply { jsonMap["is_voting_member"] = "true" }
    val currentUsername = userInfo["preferred_username"] as String?
    jsonMap["is_self"] = (requestedUsername == currentUsername).toString()
    if (jsonMap["is_self"] == "true" || isInfra) {
      requestedMember.personalEmail?.apply { jsonMap["personal_email"] = this as Any }
      requestedMember.badgeNumber?.apply { jsonMap["badge_number"] = this as Any }
      requestedMember.phoneNumber?.apply { jsonMap["phone_number"] = this as Any }
    }
    jsonMap["is_infra"] = isInfra
    jsonMap["is_voter_registration_test_mode_enabled"] =
        VoterRegistrationManager.IS_VOTER_REGISTRATION_TEST_MODE_ENABLED
    requestedMember.accountInfo?.apply {
      jsonMap["is_primary_account"] = this.isPrimaryAccount
      jsonMap["was_active_past_90_days"] = this.wasActivePast90Days ?: true
      this.lastInactiveDate?.let {
        jsonMap["last_inactive_date"] =
            it.format(LocalDate.Format { byUnicodePattern("M/d") }) as Any
        jsonMap["days_since_last_inactive"] =
            voterRegistrationManager.getDaysSinceLastInactiveDateString(it)
      }
      jsonMap["addon_accounts"] = this.addonAccounts
      this.primaryAccount?.apply {
        jsonMap["primary_account"] = this
        jsonMap["whmcs_id"] = this.whmcsId
      }
    }

    // Fetch events organized by the member
    try {
      val events = memberService.getEventsOrganizedByMember(requestedUsername, 5, session.sessionId)
      if (events.isNotEmpty()) {
        val formattedEvents =
            events.map { event ->
              mapOf(
                  "id" to event.id,
                  "name" to event.name,
                  "eventStart" to formatEventDate(event.eventStart),
                  "eventStartRaw" to event.eventStart,
                  "status" to event.status,
                  "isUpcoming" to isUpcomingEvent(event.eventStart),
                  "url" to "https://calendar.dallasmakerspace.org/events/view/${event.id}")
            }
        jsonMap["events_organized"] = formattedEvents
      }
    } catch (e: Exception) {
      log.warn("Failed to fetch events for member: $requestedUsername", e)
      // Events are optional, so we continue without them
    }

    setToastMessage(call, jsonMap, requestedMember, session)
    call.respond(ThymeleafContent("profile", jsonMap))
  }

  private fun setToastMessage(
      call: ApplicationCall,
      jsonMap: MutableMap<String, Any>,
      requestedMember: DMSMember,
      session: UserSession,
  ) {
    if (session.isDiscourseLinkSuccess) {
      log.info("Setting toast message for ${requestedMember.username} successful discourse link")
      call.sessions.set(session.copy(isDiscourseLinkSuccess = false))
      jsonMap["toast_message"] =
          "Successfully linked @${requestedMember.discourseUsername} to your profile."
      // jsonMap["toast_btn_url"] = "/unlink-discourse"
      // jsonMap["toast_btn_label"] = "Unlink"
    } else if (session.isDiscordLinkSuccess) {
      log.info("Setting toast message for ${requestedMember.username} successful discord link")
      call.sessions.set(session.copy(isDiscordLinkSuccess = false))
      jsonMap["toast_message"] =
          "Successfully linked Discord @${requestedMember.discordUsername} to your profile."
      // jsonMap["toast_btn_url"] = "/unlink-discord"
      // jsonMap["toast_btn_label"] = "Unlink"
    } else if (session.isLinkedInLinkSuccess) {
      log.info("Setting toast message for ${requestedMember.username} successful linkedin link")
      call.sessions.set(session.copy(isLinkedInLinkSuccess = false))
      jsonMap["toast_message"] =
          "Successfully linked LinkedIn ${requestedMember.linkedinUsername} to your profile."
    } else if (session.isVoterRegistrationSuccess) {
      log.info(
          "Setting toast message for ${requestedMember.username} successful voter registration")
      call.sessions.set(session.copy(isVoterRegistrationSuccess = false))
      jsonMap["toast_message"] = "Successfully registered to vote. May take few min to take effect."
      jsonMap["toast_btn_url"] = "@${requestedMember.username}/unregister-voting"
      jsonMap["toast_btn_label"] = "Unregister"
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

  /**
   * Format an event date into a human-readable string.
   *
   * @param eventStart The event start date string (e.g., "2024-03-15 14:30:00" or "2024-03-15
   *   14:30:00.0")
   * @return A human-readable string like "Fri, Mar 15 at 2:30 PM"
   */
  private fun formatEventDate(eventStart: String): String {
    return try {
      // Remove milliseconds if present (e.g., "2024-03-15 14:30:00.0" -> "2024-03-15 14:30:00")
      val cleanedEventStart = eventStart.substringBefore(".")
      val dateTime =
          LocalDateTime.parse(cleanedEventStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      val zonedDateTime = dateTime.atZone(ZoneId.of("America/Chicago"))
      zonedDateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a"))
    } catch (e: Exception) {
      log.error("Failed to parse event date: $eventStart", e)
      eventStart
    }
  }

  /**
   * Check if an event is upcoming (in the future).
   *
   * @param eventStart The event start date string
   * @return true if the event is in the future, false otherwise
   */
  private fun isUpcomingEvent(eventStart: String): Boolean {
    return try {
      val cleanedEventStart = eventStart.substringBefore(".")
      val dateTime =
          LocalDateTime.parse(cleanedEventStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      val zonedDateTime = dateTime.atZone(ZoneId.of("America/Chicago"))
      zonedDateTime.toInstant().isAfter(Instant.now())
    } catch (e: Exception) {
      false
    }
  }
}
