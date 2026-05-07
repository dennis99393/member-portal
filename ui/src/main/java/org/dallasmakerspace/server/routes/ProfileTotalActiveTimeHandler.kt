package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException

class ProfileTotalActiveTimeHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")

    try {
      val totalActiveDays = memberService.getTotalActiveDays(requestedUsername, session.sessionId)
      val result = mutableMapOf<String, Any>("status" to "ok")
      totalActiveDays?.let { days ->
        result["total_active_days"] = days
        result["total_active_duration"] = formatDurationFromDays(days)
      }
      call.respond(result)
    } catch (e: Exception) {
      log.warn("Failed to fetch total active time for member: $requestedUsername", e)
      call.respond(
          HttpStatusCode.InternalServerError,
          mapOf("error" to "Failed to fetch total active time"))
    }
  }

  @Suppress("MagicNumber")
  private fun formatDurationFromDays(totalDays: Int): String {
    if (totalDays <= 0) return "0 days"
    val years = totalDays / 365
    val remainingDaysAfterYears = totalDays % 365
    val months = remainingDaysAfterYears / 30
    val days = remainingDaysAfterYears % 30
    return when {
      years > 1 && months > 0 -> "$years years, $months months"
      years > 1 -> "$years years"
      years == 1 && months > 0 -> "$years year, $months months"
      years == 1 -> "$years year"
      months > 1 -> "$months months"
      months == 1 -> "$months month"
      days > 1 -> "$days days"
      else -> "$days day"
    }
  }
}
