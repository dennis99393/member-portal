package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.plugins.AuthException
import org.dallasmakerspace.server.reports.ReportGraph

class ReportHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    // Extract the full path from the request
    val path = call.request.path()

    // The path format is /report/<parentNode>/<childNode>
    // Split the path to get the components
    val pathComponents = path.split("/").filter { it.isNotEmpty() }

    if (pathComponents.size < 3) {
      throw AuthException("Invalid report path: $path")
    }

    // pathComponents[0] should be "report"
    val parentSlug = pathComponents[1]
    val childSlug = pathComponents[2]

    // Get the root node of the report graph
    val rootNode = ReportGraph.getRoot()

    // Find the parent node in the root's children
    val parentNode =
        rootNode.children?.find { it.urlSlug == parentSlug }
            ?: throw AuthException("Parent node not found: $parentSlug")

    // Find the child node in the parent's children
    val childNode =
        parentNode.children?.find { it.urlSlug == childSlug }
            ?: throw AuthException("Child node not found: $childSlug")

    log.info("Rendering report template for ${childNode.name}")

    val templateData =
        mutableMapOf(
            "is_infra" to isInfra,
            "fragmentList" to setOf("fragments/reports/$parentSlug/$childSlug"))

    call.respond(ThymeleafContent("report", templateData))
  }
}
