package org.dallasmakerspace.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.patch
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.dallasmakerspace.auth.ApiKeyAuthProvider
import org.dallasmakerspace.auth.apiKey
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.di.DaggerAppComponent
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.routing.Members

fun Application.configureRouting() {
  install(Resources)
  install(StatusPages) {
    exception { call: ApplicationCall, cause: Exception ->
      call.respond(
          HttpStatusCode.InternalServerError,
          ApiResponse(Status.ERROR, cause.localizedMessage, null))
    }
  }
  val apiKeyAuthProvider: ApiKeyAuthProvider.Configuration.() -> Unit = {
    // Actual validation is done in the provider, this can be refactored away
    validate { null }
  }
  install(Authentication) {
    val appConfig: AppConfig = DaggerAppComponent.create().getAppConfig()
    apiKey(appConfig, ApiKeyAuthProvider.X_API_KEY, apiKeyAuthProvider)
  }
  routing {
    val memberService: MemberService = DaggerAppComponent.create().getMemberService()
    val activityLogService: ActivityLogService = DaggerAppComponent.create().getActivityLogService()

    get("/") { call.respondRedirect("/openapi", permanent = false) }
    swaggerUI(path = "openapi")

    authenticate(ApiKeyAuthProvider.X_API_KEY) {
      get<Members.DMSMember> { memberRequested ->
        val member = memberService.getMember(memberRequested.username)
        call.respond(ApiResponse(Status.SUCCESS, "Member ${member.username}", member))
      }

      patch<Members.DMSMember.Update> { update ->
        // Update member ...
        val updatedMember = call.receive<Members.DMSMember>()
        memberService.updateMember(update.parent.username, routeObjectToModel(updatedMember))
        call.respond(
            ApiResponse(Status.SUCCESS, "Member ${update.parent.username} updated", updatedMember))
      }

      get<Members.DMSMember.ActivityLog> { activityLogRequested ->
        // Get activity log ...
        val activityLog =
            activityLogService.getMemberActivityLog(activityLogRequested.parent.username)
        call.respond(
            ApiResponse(
                Status.SUCCESS,
                "Activity log for ${activityLogRequested.parent.username}",
                activityLog))
      }
    }
  }
}

fun routeObjectToModel(it: Members.DMSMember): org.dallasmakerspace.models.DMSMember {
  return org.dallasmakerspace.models.DMSMember(
      -1,
      it.username,
      avatarUrl = it.avatarUrl,
      discourseUsername = it.discourseUsername,
      discourseAvatarUrl = it.discourseAvatarUrl,
      discordUserId = it.discordUserId,
  )
}

@Serializable data class ApiResponse<T>(val status: Status, val message: String, val data: T?)

enum class Status {
  SUCCESS,
  ERROR
}
