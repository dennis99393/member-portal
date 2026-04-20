package org.dallasmakerspace.server.remoteaccess

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.AuthenticatedHandler

class RemoteAccessHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val featureEnabled =
        memberServiceClient.getFeatureFlag("remote-access.enabled", session.sessionId)
    RemoteAccessFeatureFlag.update(featureEnabled)

    if (!featureEnabled) {
      call.respond(ThymeleafContent("remote-access-denied", mapOf("reason" to "disabled")))
      return
    }

    val rawCategories = memberServiceClient.getRemoteAccessCategories(session.sessionId)
    val userGroups =
        ((userInfo["groups"] as? List<*>) ?: emptyList<String>()).filterIsInstance<String>()

    val categories =
        rawCategories.map { category ->
          val requiredAdGroup = category["requiredAdGroup"] as? String ?: ""
          val userHasAccess =
              requiredAdGroup.isBlank() ||
                  userGroups.any { it.trimStart('/') == requiredAdGroup.trimStart('/') }

          @Suppress("UNCHECKED_CAST")
          val machines =
              (category["machines"] as? List<Map<String, Any?>>) ?: emptyList<Map<String, Any?>>()

          mapOf(
              "slug" to (category["slug"] as? String ?: ""),
              "displayName" to (category["displayName"] as? String ?: ""),
              "description" to (category["description"] as? String ?: ""),
              "requiredAdGroup" to requiredAdGroup,
              "userHasAccess" to userHasAccess,
              "machines" to machines,
          )
        }

    call.respond(ThymeleafContent("remote-access", mapOf("categories" to categories)))
  }
}
