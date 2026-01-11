package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService

/**
 * Handler for the short links admin management page. Provides access to namespace and link
 * management based on user permissions.
 */
class ShortLinksAdminHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
    private val appConfig: AppConfig,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    // Check if development mode is enabled
    if (!appConfig.isDevelopmentMode()) {
      log.warn(
          "Short-links admin access denied - not in development mode. User: ${userInfo["preferred_username"]}")
      call.respondText(
          "Short Links Management is currently in development and only available in development mode.",
          status = HttpStatusCode.Forbidden)
      return
    }

    // MVP: Only Infrastructure team can access admin page
    if (!isInfra) {
      log.warn("Access denied to short-links admin for user: ${userInfo["preferred_username"]}")
      call.respondText("Access denied", status = HttpStatusCode.Forbidden)
      return
    }

    // Determine user permissions based on groups
    val canManageNamespaces = isInfra
    val canManageAnyLink = isInfra

    val username = userInfo["preferred_username"] as String?
    val displayName = userInfo["name"] as String?

    // Build context for template
    val jsonMap =
        mutableMapOf<String, Any>(
            "username" to (username ?: ""),
            "display_name" to (displayName ?: ""),
            "can_manage_namespaces" to canManageNamespaces,
            "can_manage_any_link" to canManageAnyLink,
            "is_infra" to isInfra,
            "is_officer" to isOfficer)

    // Respond with Thymeleaf template
    call.respond(ThymeleafContent("short-links-admin", jsonMap))
  }
}
