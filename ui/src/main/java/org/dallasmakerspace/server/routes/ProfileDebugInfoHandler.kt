package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.plugins.AuthException

class ProfileDebugInfoHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    if (!isInfra) {
      call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
      return
    }

    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")

    try {
      val debugInfo = memberService.getDebugInfo(requestedUsername, session.sessionId)
      if (debugInfo == null) {
        call.respond(mapOf("status" to "empty"))
        return
      }

      val result =
          mutableMapOf<String, Any>(
              "status" to "ok",
              "ad_account_enabled" to debugInfo.adAccountEnabled,
              "mm_ad_active" to debugInfo.mmAdActive,
              "whmcs_active" to debugInfo.whmcsActive,
          )
      debugInfo.daysInCurrentWhmcsStatus?.let { result["days_in_current_whmcs_status"] = it }
      debugInfo.totalActiveDays?.let { days ->
        result["total_active_days"] = days
        result["total_active_duration"] = formatDurationFromDays(days)
      }
      if (debugInfo.timeline.isNotEmpty()) {
        result["product_timeline"] =
            debugInfo.timeline.map { entry ->
              mapOf(
                  "type" to entry.type.name,
                  "start_date" to entry.startDate.toString(),
                  "end_date" to (entry.endDate?.toString() ?: "Present"),
                  "duration_days" to entry.durationDays,
              )
            }
      }
      call.respond(result)
    } catch (e: Exception) {
      log.warn("Failed to fetch debug info for member: $requestedUsername", e)
      call.respond(
          HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch debug info"))
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
