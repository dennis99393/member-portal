package org.dallasmakerspace.server.remoteaccess

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.AuthenticatedHandler

class RemoteAccessDisconnectHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val connectionId =
        call.parameters["connectionId"]
            ?: run {
              call.respond(HttpStatusCode.BadRequest)
              return
            }

    val rawCategories = memberServiceClient.getRemoteAccessCategories(session.sessionId)
    val machine =
        rawCategories
            .flatMap { cat ->
              @Suppress("UNCHECKED_CAST")
              (cat["machines"] as? List<Map<String, Any?>>) ?: emptyList()
            }
            .firstOrNull { it["connectionId"] as? String == connectionId }

    val activeConnectionId = machine?.get("activeConnectionId") as? String
    if (activeConnectionId == null) {
      call.respond(HttpStatusCode.NoContent)
      return
    }

    memberServiceClient.killActiveConnection(activeConnectionId, session.sessionId)
    call.respond(HttpStatusCode.NoContent)
  }
}
