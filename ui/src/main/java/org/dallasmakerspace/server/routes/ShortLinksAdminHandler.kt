package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.Permission
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
    if (!appConfig.isDevelopmentMode()) {
      log.warn(
          "Short-links admin access denied - not in development mode. User: ${userInfo["preferred_username"]}")
      call.respondText(
          "Short Links Management is currently in development and only available in development mode.",
          status = HttpStatusCode.Forbidden)
      return
    }

    if (!call.require(Permission.MANAGE_SHORTLINKS)) return

    val username = userInfo["preferred_username"] as String?
    val displayName = userInfo["name"] as String?

    call.respond(
        ThymeleafContent(
            "short-links-admin",
            mapOf(
                "username" to (username ?: ""),
                "display_name" to (displayName ?: ""),
                "authz" to authz,
            ),
        ))
  }
}
