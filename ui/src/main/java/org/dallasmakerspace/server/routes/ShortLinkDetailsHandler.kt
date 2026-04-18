package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService

/**
 * Handler for the short link details page. Shows QR code, click statistics, and recent activity.
 */
class ShortLinkDetailsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val linkId = call.parameters["id"]?.toIntOrNull()

    if (linkId == null) {
      log.warn("Invalid link ID in path")
      call.respondText("Invalid link ID", status = HttpStatusCode.BadRequest)
      return
    }

    val username = userInfo["preferred_username"] as String?
    val displayName = userInfo["name"] as String?

    call.respond(
        ThymeleafContent(
            "short-link-details",
            mapOf(
                "username" to (username ?: ""),
                "display_name" to (displayName ?: ""),
                "link_id" to linkId,
                "authz" to authz,
            ),
        ))
  }
}
