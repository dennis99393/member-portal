package org.dallasmakerspace.server.routes

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
 * Handler for the short links public page. Shows popular short links and provides access to the
 * admin panel for authorized users.
 */
class ShortLinksHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
    private val appConfig: AppConfig,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val username = userInfo["preferred_username"] as String?
    val displayName = userInfo["name"] as String?
    val isDevelopmentMode = appConfig.isDevelopmentMode()
    val canAccessAdmin = authz.can(Permission.MANAGE_SHORTLINKS.name) && isDevelopmentMode

    call.respond(
        ThymeleafContent(
            "short-links",
            mapOf(
                "username" to (username ?: ""),
                "display_name" to (displayName ?: ""),
                "can_access_admin" to canAccessAdmin,
                "is_development_mode" to isDevelopmentMode,
                "authz" to authz,
            ),
        ))
  }
}
