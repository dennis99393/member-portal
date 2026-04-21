package org.dallasmakerspace.server.remoteaccess

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.Permission
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.AuthenticatedHandler

class RemoteAccessConnectHandler
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

    val isInfraUser = authz.can(Permission.MANAGE_CONFIG.name)
    val featureEnabled =
        memberServiceClient.getFeatureFlag("remote-access.enabled", session.sessionId, isInfraUser)
    RemoteAccessFeatureFlag.update(featureEnabled, isInfraUser)

    if (!featureEnabled) {
      call.respond(HttpStatusCode.Forbidden)
      return
    }

    val rawCategories = memberServiceClient.getRemoteAccessCategories(session.sessionId)
    val userGroups =
        ((userInfo["groups"] as? List<*>) ?: emptyList<String>()).filterIsInstance<String>()

    val matchingCategory =
        rawCategories.firstOrNull { category ->
          @Suppress("UNCHECKED_CAST")
          val machines = (category["machines"] as? List<Map<String, Any?>>) ?: emptyList()
          machines.any { it["connectionId"] as? String == connectionId }
        }

    if (matchingCategory == null) {
      call.respond(HttpStatusCode.NotFound)
      return
    }

    val requiredAdGroup = matchingCategory["requiredAdGroup"] as? String ?: ""
    val userHasAccess =
        requiredAdGroup.isBlank() ||
            userGroups.any { it.trimStart('/') == requiredAdGroup.trimStart('/') }

    if (!userHasAccess) {
      call.respond(HttpStatusCode.Forbidden)
      return
    }

    val guacClientId = GuacamoleClientId.encode(connectionId)
    call.respondRedirect("/guacamole/#/client/$guacClientId")
  }
}
