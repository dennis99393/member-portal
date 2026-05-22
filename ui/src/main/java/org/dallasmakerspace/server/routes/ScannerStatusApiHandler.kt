package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient

class ScannerStatusApiHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val username =
        call.parameters["username"]
            ?: return call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing username"))
    val actorUsername = session.userId
    val data =
        try {
          memberServiceClient.getScannerStatus(username, session.sessionId, actorUsername)
        } catch (ex: Exception) {
          log.error("Scanner status service error for $username", ex)
          return call.respond(
              HttpStatusCode.BadGateway, mapOf("error" to "Service unavailable, please try again"))
        }
    if (data == null) {
      call.respond(HttpStatusCode.NotFound, mapOf("error" to "Member not found: $username"))
    } else {
      call.respond(data)
    }
  }
}
