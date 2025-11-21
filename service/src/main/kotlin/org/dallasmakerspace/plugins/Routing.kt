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
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.dallasmakerspace.auth.ApiKeyAuthProvider
import org.dallasmakerspace.auth.apiKey
import org.dallasmakerspace.auth.requireRole
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.cron.MemberRefreshCronJob
import org.dallasmakerspace.cron.MemberRefreshCronJobParams
import org.dallasmakerspace.dataviz.DataVizRouter
import org.dallasmakerspace.di.DaggerAppComponent
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.members.GroupService
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.routing.BadgeLookup
import org.dallasmakerspace.routing.Groups
import org.dallasmakerspace.routing.Members
import org.dallasmakerspace.webhook.WebhookRouter

@Suppress("LongMethod")
fun Application.configureRouting() {
  val log = org.slf4j.LoggerFactory.getLogger("Routing")
  install(Resources)
  install(StatusPages) {
    exception { call: ApplicationCall, cause: Exception ->
      log.error("Failed to process request", cause)
      call.respond(
          HttpStatusCode.InternalServerError,
          ApiResponse(Status.ERROR, cause.localizedMessage, null),
      )
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
    val memberService: MemberService by lazy { DaggerAppComponent.create().getMemberService() }
    val groupsService: GroupService by lazy { DaggerAppComponent.create().getGroupService() }
    val activityLogService: ActivityLogService by lazy {
      DaggerAppComponent.create().getActivityLogService()
    }
    val memberRefreshCronJob: MemberRefreshCronJob by lazy {
      DaggerAppComponent.create().getMemberRefreshCronJob()
    }
    val dataVizRouter: DataVizRouter by lazy { DaggerAppComponent.create().getDataVizRouter() }
    val webhookRouter: WebhookRouter by lazy { DaggerAppComponent.create().getWebhookRouter() }

    get("/") {
      call.respondText(
          "<html><body style='font-family:Arial,sans-serif'><h2>API Documentation:</h2>" +
              "<ul><li>member-profile-service - <a href='openapi'>/openapi</a></li>" +
              "<li>badge-lookup-service - <a href='badge-lookup/openapi'>/badge-lookup/openai</a></li>" +
              "</ul></body></html>",
          ContentType.Text.Html,
      )
    }
    swaggerUI(path = "openapi")
    swaggerUI(
        path = "badge-lookup/openapi",
        swaggerFile = "openapi/documentation-badge-lookup.yaml",
    )

    authenticate(ApiKeyAuthProvider.X_API_KEY) {

      /** Member profile operations - READ * */
      requireRole("member:read") {
        get<Members> { members ->
          val loggedInDays = members.loggedInDays
          // Use the new getAllMembers method instead of getMembersLoggedInDays
          val memberList = memberService.getAllMembers()
          call.respond(ApiResponse(Status.SUCCESS, "All members: ${memberList.size}", memberList))
        }

        get<Members.DMSMember> { memberRequested ->
          val member = memberService.getMemberByUsername(memberRequested.username)
          call.respond(ApiResponse(Status.SUCCESS, "Member ${member.username}", member))
        }

        /** Activity Log operations - requires member:read since it's member data * */
        get<Members.DMSMember.ActivityLog> { activityLogRequested ->
          // Get activity log ...
          val activityLog =
              activityLogService.getMemberActivityLog(activityLogRequested.parent.username)
          call.respond(
              ApiResponse(
                  Status.SUCCESS,
                  "Activity log for ${activityLogRequested.parent.username}",
                  activityLog,
              )
          )
        }
      }

      /** Member profile operations - WRITE * */
      requireRole("member:write") {
        patch<Members.DMSMember.Update> { update ->
          // Update member ...
          val updatedMember = call.receive<Members.DMSMember>()
          memberService.updateMember(update.parent.username, routeObjectToModel(updatedMember))
          call.respond(
              ApiResponse(Status.SUCCESS, "Member ${update.parent.username} updated", updatedMember)
          )
        }
      }

      /** Group operations - READ * */
      requireRole("group:read") {
        get<Groups> {
          val groupsList = groupsService.getAllGroups()
          call.respond(ApiResponse(Status.SUCCESS, "All groups: ${groupsList.size}", groupsList))
        }

        get<Groups.DMSGroup> { groupRequested ->
          val group = memberService.getGroup(groupRequested.groupslug)
          call.respond(ApiResponse(Status.SUCCESS, "Group ${group.name}", group))
        }
      }

      /** Group operations - WRITE * */
      requireRole("group:write") {
        patch<Groups.DMSGroup.Add> { groupRequested ->
          // Update group ...
          val memberUsername = call.receive<String>()
          val groupslug = groupRequested.parent.groupslug
          memberService.addMembersToGroup(listOf(memberUsername), groupslug)
          call.respond(
              ApiResponse(
                  Status.SUCCESS,
                  "Added member to $groupslug updated: $memberUsername",
                  null,
              )
          )
        }

        delete<Groups.DMSGroup.Add> { groupRequested ->
          // Update group ...
          val memberUsername = call.receive<String>()
          val groupslug = groupRequested.parent.groupslug
          memberService.removeMembersToGroup(listOf(memberUsername), groupslug)
          call.respond(
              ApiResponse(
                  Status.SUCCESS,
                  "Removed member to $groupslug updated: $memberUsername",
                  null,
              )
          )
        }
      }

      /** Badge lookup operations * */
      requireRole("badge:read") {
        get<BadgeLookup> {
          val member = memberService.getMemberByBadgeNumber(it.badgeNumber)
          call.respond(ApiResponse(Status.SUCCESS, "Member ${member.username}", member))
        }
      }

      /** Administrative cron operations * */
      requireRole("cron:execute") {
        get("/cron/member-refresh") {
          // Get query string param for isRunningInShadowMode
          val isRunningInShadowMode =
              call.request.queryParameters["isRunningInShadowMode"]?.toBoolean() ?: true
          val params = MemberRefreshCronJobParams(isRunningInShadowMode)
          val result = memberRefreshCronJob.run(params)
          call.respond(result)
        }
      }

      /** Data visualization reports * */
      requireRole("dataviz:read") {
        get("/data-viz/*") {
          val method = call.request.path().substringAfter("/data-viz/")
          val params = call.request.queryParameters.toMap()
          val respone = dataVizRouter.route(method, params)
          call.respond(ApiResponse(Status.SUCCESS, "Backend API $method", respone))
        }
      }
    }
    post("/webhook/*") {
      val path = call.request.path().substringAfter("/webhook/")
      val body = call.receiveText()

      // Log request details for debugging
      log.info(
          "Webhook request received: path=${call.request.path()}, " +
              "query params=${call.request.queryParameters.entries().map { "${it.key}:${it.value}" }
                .joinToString { ";" }}, " +
              "headers=${call.request.headers.entries().map{ "${it.key}:${it.value}" }.joinToString { ";" }}"
      )

      // Route the webhook to appropriate handler
      val result = webhookRouter.route(path, body)

      if (result.success) {
        call.respond(ApiResponse(Status.SUCCESS, result.message, null))
      } else {
        call.respond(HttpStatusCode.BadRequest, ApiResponse(Status.ERROR, result.message, null))
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
  ERROR,
}
