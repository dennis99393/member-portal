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
import org.dallasmakerspace.cron.DoorSwipesCronJob
import org.dallasmakerspace.cron.DoorSwipesCronJobParams
import org.dallasmakerspace.cron.MemberRefreshCronJob
import org.dallasmakerspace.cron.MemberRefreshCronJobParams
import org.dallasmakerspace.cron.ShowAndTellCronJob
import org.dallasmakerspace.cron.ShowAndTellCronJobParams
import org.dallasmakerspace.dataviz.DataVizRouter
import org.dallasmakerspace.di.DaggerAppComponent
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.members.GroupService
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.routing.BadgeLookup
import org.dallasmakerspace.routing.Groups
import org.dallasmakerspace.routing.Members
import org.dallasmakerspace.routing.ShortLinksResource
import org.dallasmakerspace.routing.*
import org.dallasmakerspace.shortlinks.RedirectResult
import org.dallasmakerspace.shortlinks.ShortLinksService
import org.dallasmakerspace.webhook.WebhookRouter
import org.dallasmakerspace.models.NamespaceOwnerType
import org.dallasmakerspace.members.db.ProfileDAO
import org.dallasmakerspace.members.db.suspendTransaction

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
    val calendarService by lazy { DaggerAppComponent.create().getCalendarService() }
    val memberRefreshCronJob: MemberRefreshCronJob by lazy {
      DaggerAppComponent.create().getMemberRefreshCronJob()
    }
    val showAndTellCronJob: ShowAndTellCronJob by lazy {
      DaggerAppComponent.create().getShowAndTellCronJob()
    }
    val doorSwipesCronJob: DoorSwipesCronJob by lazy {
      DaggerAppComponent.create().getDoorSwipesCronJob()
    }
    val dataVizRouter: DataVizRouter by lazy { DaggerAppComponent.create().getDataVizRouter() }
    val webhookRouter: WebhookRouter by lazy { DaggerAppComponent.create().getWebhookRouter() }
    val shortLinksService: ShortLinksService by lazy {
      DaggerAppComponent.create().getShortLinksService()
    }

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

        /** Calendar Events operations - requires member:read since it's member data * */
        get<Members.DMSMember.Events> { eventsRequested ->
          // Get events organized by member ...
          val events =
              calendarService.getEventsOrganizedByMember(eventsRequested.parent.username, eventsRequested.limit)
          call.respond(
              ApiResponse(
                  Status.SUCCESS,
                  "Events organized by ${eventsRequested.parent.username}",
                  events,
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

        get("/cron/show-and-tell") {
          val isRunningInShadowMode =
              call.request.queryParameters["isRunningInShadowMode"]?.toBoolean() ?: true
          val params = ShowAndTellCronJobParams(isRunningInShadowMode)
          val result = showAndTellCronJob.run(params)
          call.respond(result)
        }

        get("/cron/door-swipes") {
          val params = DoorSwipesCronJobParams()
          val result = doorSwipesCronJob.run(params)
          call.respond(result)
        }
      }


      /** Data visualization reports * */
      requireRole("dataviz:read") {
        get("/data-viz/*") {
          val method = call.request.path().substringAfter("/data-viz/")

          // Check for cache override in query parameter (e.g., ?cache=0 to bypass cache)
          val bypassCache = call.request.queryParameters["cache"] == "0"

          if (bypassCache) {
            // Skip cache for this request
            call.response.headers.append("Cache-Control", "no-cache")
          }

          val params =
              call.request.queryParameters.names()
                  .filter { it != "cache" } // Exclude cache parameter from report params
                  .associateWith { paramName ->
                    call.request.queryParameters.getAll(paramName) ?: emptyList()
                  }
          val respone = dataVizRouter.route(method, params)
          call.respond(ApiResponse(Status.SUCCESS, "Backend API $method", respone))
        }
      }

      /** Short Links Routes - requires authentication * */
      requireRole("shortlinks:read") {
        get<ShortLinksResource.Namespaces> {
          val namespaces = shortLinksService.getAllNamespaces()
          call.respond(ApiResponse(Status.SUCCESS, "Namespaces retrieved", namespaces))
        }

        get<ShortLinksResource.Namespaces.ById> { request ->
          val namespace = shortLinksService.getNamespace(request.id)
          if (namespace != null) {
            call.respond(ApiResponse(Status.SUCCESS, "Namespace found", namespace))
          } else {
            call.respond(
                HttpStatusCode.NotFound, ApiResponse(Status.ERROR, "Namespace not found", null))
          }
        }

        get<ShortLinksResource.Links> {
          val links = shortLinksService.getAllShortLinks()
          call.respond(ApiResponse(Status.SUCCESS, "Short links retrieved", links))
        }

        get<ShortLinksResource.Links.ById> { request ->
          val link = shortLinksService.getShortLink(request.id)
          if (link != null) {
            call.respond(ApiResponse(Status.SUCCESS, "Short link found", link))
          } else {
            call.respond(
                HttpStatusCode.NotFound, ApiResponse(Status.ERROR, "Short link not found", null))
          }
        }

        get<ShortLinksResource.Links.ByNamespace> { request ->
          val links = shortLinksService.getAllShortLinks(namespaceId = request.namespaceId)
          call.respond(ApiResponse(Status.SUCCESS, "Namespace links retrieved", links))
        }

        get("/short-links/links/popular") {
          val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
          val popularLinks = shortLinksService.getPopularShortLinks(limit)
          call.respond(
              ApiResponse(
                  Status.SUCCESS,
                  "Popular short links retrieved",
                  popularLinks.map { (link, clicks) -> PopularShortLinkResponse(link, clicks) }))
        }
      }

      requireRole("shortlinks:write") {
        post("/short-links/namespaces/create") {
          val request = call.receive<CreateNamespaceRequest>()
          val username = call.request.headers["X-Username"]
          val creatorProfileId = username?.let {
            suspendTransaction { ProfileDAO.findById(it)?.idColumn?.value }
          }

          val result = shortLinksService.createNamespace(
              name = request.name,
              ownerType = when (request.ownerType) {
                "committee" -> NamespaceOwnerType.COMMITTEE
                "system" -> NamespaceOwnerType.SYSTEM
                else -> throw IllegalArgumentException("Invalid owner type")
              },
              ownerGroupId = request.ownerGroupId,
              description = request.description,
              primaryAlias = request.primaryAlias,
              additionalAliases = request.additionalAliases,
              createdBy = creatorProfileId)

          result.fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Namespace created", it)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Failed to create namespace", null)) })
        }

        patch("/short-links/namespaces/{id}/update") {
          val id = call.parameters["id"]?.toIntOrNull()
              ?: return@patch call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, "Invalid namespace ID", null))

          val updates = call.receive<UpdateNamespaceRequest>()
          val existing = shortLinksService.getNamespace(id)
              ?: return@patch call.respond(HttpStatusCode.NotFound,
                  ApiResponse(Status.ERROR, "Namespace not found", null))

          val updated = existing.copy(
              name = updates.name, description = updates.description, isActive = updates.isActive)

          shortLinksService.updateNamespace(id, updated).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Namespace updated", it)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Update failed", null)) })
        }

        delete("/short-links/namespaces/{id}/delete") {
          val id = call.parameters["id"]?.toIntOrNull()
              ?: return@delete call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, "Invalid namespace ID", null))

          shortLinksService.deleteNamespace(id).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Namespace deleted", null)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Delete failed", null)) })
        }

        post("/short-links/namespaces/{id}/aliases/add") {
          val id = call.parameters["id"]?.toIntOrNull()
              ?: return@post call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, "Invalid namespace ID", null))

          val aliasRequest = call.receive<AddAliasRequest>()
          shortLinksService.addAlias(id, aliasRequest.alias, aliasRequest.isPrimary).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Alias added", it)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Failed to add alias", null)) })
        }

        post("/short-links/links/create") {
          val request = call.receive<CreateShortLinkRequest>()
          val username = call.request.headers["X-Username"]
          val creatorProfileId = username?.let {
            suspendTransaction { ProfileDAO.findById(it)?.idColumn?.value }
          }

          shortLinksService.createShortLink(
              namespaceId = request.namespaceId,
              slug = request.slug,
              destinationUrl = request.destinationUrl,
              description = request.description,
              creatorId = creatorProfileId).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Short link created", it)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Failed to create link", null)) })
        }

        patch("/short-links/links/{id}/update") {
          val id = call.parameters["id"]?.toIntOrNull()
              ?: return@patch call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, "Invalid link ID", null))

          val username = call.request.headers["X-Username"]
          val updaterProfileId = username?.let {
            suspendTransaction { ProfileDAO.findById(it)?.idColumn?.value }
          }

          val updates = call.receive<UpdateShortLinkRequest>()
          val existing = shortLinksService.getShortLink(id)
              ?: return@patch call.respond(HttpStatusCode.NotFound,
                  ApiResponse(Status.ERROR, "Short link not found", null))

          val updated = existing.copy(
              slug = updates.slug,
              destinationUrl = updates.destinationUrl,
              description = updates.description,
              isActive = updates.isActive,
              updatedBy = updaterProfileId)

          shortLinksService.updateShortLink(id, updated).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Short link updated", it)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Update failed", null)) })
        }

        delete("/short-links/links/{id}/delete") {
          val id = call.parameters["id"]?.toIntOrNull()
              ?: return@delete call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, "Invalid link ID", null))

          shortLinksService.deleteShortLink(id).fold(
              onSuccess = { call.respond(ApiResponse(Status.SUCCESS, "Short link deleted", null)) },
              onFailure = { call.respond(HttpStatusCode.BadRequest,
                  ApiResponse(Status.ERROR, it.message ?: "Delete failed", null)) })
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

    /** Short Links - Public Redirect Handler * */
    get("/go/{path...}") {
      val path = call.parameters.getAll("path")?.joinToString("/") ?: ""

      when (val result = shortLinksService.resolveRedirect(path)) {
        is RedirectResult.Success -> {
          call.respondRedirect(result.destinationUrl, permanent = false)
        }
        is RedirectResult.NotFound -> {
          call.respond(
              HttpStatusCode.NotFound, ApiResponse(Status.ERROR, result.message, null))
        }
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

@Serializable
enum class Status {
  SUCCESS,
  ERROR,
}
