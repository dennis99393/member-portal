package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.models.Committees
import org.dallasmakerspace.models.Tools
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.models.getSlugFromName

class InterlockToolsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val toolSlug = call.request.queryParameters["tool"]

    if (toolSlug == null) {
      // List view
      val toolsByCommittee =
          Tools.byCommittee().map { (committee, tools) ->
            mapOf(
                "committeeName" to committee.name,
                "committeeSlug" to committee.slug,
                "tools" to
                    tools.map { tool ->
                      mapOf(
                          "name" to tool.name,
                          "interlockTag" to tool.interlockTag,
                          "prerequisiteGroup" to tool.prerequisiteGroup,
                          "prerequisiteGroupSlug" to
                              tool.prerequisiteGroup?.let { getSlugFromName(it) },
                          "timeoutSeconds" to tool.timeoutSeconds,
                      )
                    },
            )
          }
      call.respond(
          ThymeleafContent("interlock-tools", mapOf("toolsByCommittee" to toolsByCommittee)))
    } else {
      // Detail / report view
      val tool = Tools.findBySlug(toolSlug)
      if (tool == null) {
        call.respond(HttpStatusCode.NotFound)
        return
      }
      val committee = Committees.findById(tool.committeeId)
      val model =
          mapOf(
              "toolName" to tool.name,
              "toolTag" to tool.interlockTag,
              "committeeName" to (committee?.name ?: ""),
              "committeeSlug" to (committee?.slug ?: ""),
              "prerequisiteGroup" to tool.prerequisiteGroup,
              "prerequisiteGroupSlug" to tool.prerequisiteGroup?.let { getSlugFromName(it) },
              "timeoutSeconds" to tool.timeoutSeconds,
          )
      call.respond(ThymeleafContent("interlock-tool-detail", model))
    }
  }
}
