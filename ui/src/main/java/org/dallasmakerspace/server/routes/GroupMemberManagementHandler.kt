package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dallasmakerspace.server.auth.Permission
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService

class GroupMemberManagementHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  private val log = loggerFactory.create(javaClass)

  @Serializable data class AddMembersRequest(val usernames: List<String>)

  @Serializable data class ApiResponse(val success: Boolean, val message: String)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    if (!call.require(Permission.MANAGE_GROUPS)) return

    val groupSlug =
        call.parameters["group_slug"]
            ?: run {
              call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Missing group slug"))
              return
            }

    val actorUsername = userInfo["preferred_username"] as? String

    when (call.request.httpMethod) {
      HttpMethod.Post -> {
        val body =
            try {
              Json.decodeFromString<AddMembersRequest>(call.receiveText())
            } catch (e: Exception) {
              call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid request body"))
              return
            }
        if (body.usernames.isEmpty()) {
          call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "No usernames provided"))
          return
        }
        try {
          body.usernames.forEach { username ->
            memberService.addToGroup(session.sessionId, username, groupSlug, actorUsername)
          }
          call.respond(
              HttpStatusCode.OK,
              ApiResponse(true, "Added ${body.usernames.size} member(s) to group"),
          )
        } catch (e: Exception) {
          log.error("Failed to add members to group $groupSlug", e)
          call.respond(
              HttpStatusCode.InternalServerError,
              ApiResponse(false, e.message ?: "Failed to add members"),
          )
        }
      }
      HttpMethod.Delete -> {
        val username =
            call.parameters["username"]?.takeIf { it.isNotBlank() }
                ?: run {
                  call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Missing username"))
                  return
                }
        try {
          memberService.removeFromGroup(session.sessionId, username, groupSlug, actorUsername)
          call.respond(HttpStatusCode.OK, ApiResponse(true, "Removed $username from group"))
        } catch (e: Exception) {
          log.error("Failed to remove $username from group $groupSlug", e)
          call.respond(
              HttpStatusCode.InternalServerError,
              ApiResponse(false, e.message ?: "Failed to remove member"),
          )
        }
      }
      else ->
          call.respond(HttpStatusCode.MethodNotAllowed, ApiResponse(false, "Method not allowed"))
    }
  }
}
