package org.dallasmakerspace.server.remoteaccess

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
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

    val featureEnabled =
        memberServiceClient.getFeatureFlag("remote-access.enabled", session.sessionId)
    RemoteAccessFeatureFlag.update(featureEnabled)

    if (!featureEnabled) {
      call.respond(
          HttpStatusCode.Forbidden,
          ThymeleafContent("remote-access-denied", mapOf("reason" to "disabled")))
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
      call.respond(
          HttpStatusCode.NotFound,
          ThymeleafContent("remote-access-denied", mapOf("reason" to "unknown-connection")))
      return
    }

    val requiredAdGroup = matchingCategory["requiredAdGroup"] as? String ?: ""
    val userHasAccess =
        requiredAdGroup.isBlank() ||
            userGroups.any { it.trimStart('/') == requiredAdGroup.trimStart('/') }

    if (!userHasAccess) {
      call.respond(
          HttpStatusCode.Forbidden,
          ThymeleafContent(
              "remote-access-denied",
              mapOf("reason" to "forbidden", "requiredAdGroup" to requiredAdGroup)))
      return
    }

    val guacClientId = GuacamoleClientId.encode(connectionId)
    @Suppress("UNCHECKED_CAST")
    val machines = (matchingCategory["machines"] as? List<Map<String, Any?>>) ?: emptyList()
    val machine = machines.firstOrNull { it["connectionId"] as? String == connectionId }
    val machineName = machine?.get("displayName") as? String ?: connectionId

    call.respond(
        ThymeleafContent(
            "remote-access-connect",
            mapOf(
                "guacClientId" to guacClientId,
                "machineName" to machineName,
                "connectionId" to connectionId,
            )))
  }
}
