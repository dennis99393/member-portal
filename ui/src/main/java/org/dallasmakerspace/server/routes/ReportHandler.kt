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
import org.dallasmakerspace.server.reports.ReportNode

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

    // Get the root node of the report graph
    val rootNode = ReportGraph.getRoot()

    // The path format is /report/<parentNode>/<childNode>
    // Split the path to get the components
    val pathComponents = path.split("/").filter { it.isNotEmpty() }

    // Handle root reports page
    if (pathComponents.size == 1 && pathComponents[0] == "reports") {
      renderReportsList(call, rootNode, null, null)
      return
    }

    // Handle parent node page
    if (pathComponents.size == 2 && pathComponents[0] == "reports") {
      val parentSlug = pathComponents[1]
      val parentNode =
          rootNode.children?.find { it.urlSlug == parentSlug }
              ?: throw AuthException("Parent node not found: $parentSlug")

      renderReportsList(call, rootNode, parentNode, null)
      return
    }

    // Handle child node page (specific report)
    if (pathComponents.size < 3) {
      throw AuthException("Invalid report path: $path")
    }

    // pathComponents[0] should be "reports"
    val parentSlug = pathComponents[1]
    val childSlug = pathComponents[2]

    // Find the parent node in the root's children
    val parentNode =
        rootNode.children?.find { it.urlSlug == parentSlug }
            ?: throw AuthException("Parent node not found: $parentSlug")

    // Find the child node in the parent's children
    val childNode =
        parentNode.children?.find { it.urlSlug == childSlug }
            ?: throw AuthException("Child node not found: $childSlug")

    log.info("Rendering report template for ${childNode.name}")

    // Extract query parameters to pass to template
    val queryParams =
        call.request.queryParameters.names().associateWith { paramName ->
          call.request.queryParameters[paramName] ?: ""
        }

    val templateData =
        mutableMapOf(
            "is_infra" to isInfra,
            "fragmentList" to setOf("fragments/reports/$parentSlug/$childSlug"),
            "parentNode" to parentNode,
            "childNode" to childNode,
            "reportTitle" to "${childNode.name} - Reports",
            "showChildren" to false,
            "queryParams" to queryParams,
        )

    call.respond(ThymeleafContent("report", templateData))
  }

  private suspend fun renderReportsList(
      call: ApplicationCall,
      rootNode: ReportNode,
      parentNode: ReportNode?,
      childNode: ReportNode?,
  ) {
    val nodesToDisplay =
        when {
          childNode != null && childNode.children != null -> childNode.children
          parentNode != null && parentNode.children != null -> parentNode.children
          else -> rootNode.children
        }

    // Filter out reports that support query params
    val visibleNodes = nodesToDisplay?.filter { it.visibleInNav }

    val currentNodeName = childNode?.name ?: parentNode?.name ?: "Reports"

    val templateData =
        mutableMapOf(
            "is_infra" to isInfra,
            "childrenNodes" to visibleNodes,
            "showChildren" to true,
            "reportTitle" to "$currentNodeName - Reports",
        )

    if (parentNode != null) {
      templateData["parentNode"] = parentNode
    }

    if (childNode != null) {
      templateData["childNode"] = childNode
    }

    log.info("Rendering reports list for $currentNodeName")
    call.respond(ThymeleafContent("report", templateData as Map<String, Any>))
  }
}
