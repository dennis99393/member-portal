package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import javax.inject.Inject

/**
 * Handler for the short links public page.
 * Shows popular short links and provides access to the admin panel for authorized users.
 */
class ShortLinksHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
    private val log = loggerFactory.create(javaClass)

    override suspend fun handleAuthenticated(call: ApplicationCall) {
        val username = userInfo["preferred_username"] as String?
        val displayName = userInfo["name"] as String?
        val canAccessAdmin = isInfra || isOfficer

        // Build context for template
        val jsonMap = mutableMapOf<String, Any>(
            "username" to (username ?: ""),
            "display_name" to (displayName ?: ""),
            "can_access_admin" to canAccessAdmin,
            "is_infra" to isInfra,
            "is_officer" to isOfficer
        )

        // Respond with Thymeleaf template
        call.respond(ThymeleafContent("short-links", jsonMap))
    }
}
