package org.dallasmakerspace.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
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

    get("/") { call.respondRedirect("/openapi", permanent = false) }

    authenticate(ApiKeyAuthProvider.X_API_KEY) {
      get<Members.DMSMember> { memberRequest ->
        val member = memberService.getMember(memberRequest.username)
        call.respond(ApiResponse(Status.SUCCESS, "Member ${memberRequest.username}", member))
      }
    }

    patch<Members.DMSMember.Update> { update ->
      // Update member ...
      val updatedMember = call.receive<Members.DMSMember>()
      memberService.updateMember(update.parent.username, routeObjectToModel(updatedMember))
      call.respond(
          ApiResponse(Status.SUCCESS, "Member ${update.parent.username} updated", updatedMember))
    }
  }
}

fun routeObjectToModel(it: Members.DMSMember): org.dallasmakerspace.models.DMSMember {
  return org.dallasmakerspace.models.DMSMember(
      it.username,
      discourseUsername = it.discourseUsername,
      discourseAvatarUrl = it.discourseAvatarUrl)
}

@Serializable data class ApiResponse<T>(val status: Status, val message: String, val data: T?)

enum class Status {
  SUCCESS,
  ERROR
}
