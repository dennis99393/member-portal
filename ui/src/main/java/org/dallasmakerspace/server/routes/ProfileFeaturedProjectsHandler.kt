package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.plugins.AuthException

class ProfileFeaturedProjectsHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) = coroutineScope {
    val requestedUsername =
        call.parameters["preferred_username"]
            ?: throw AuthException("No username found in url path")

    try {
      val projects =
          memberServiceClient.getFeaturedProjectsForMemberAsync(
              requestedUsername, session.sessionId)
      call.respond(
          projects.map { p ->
            mapOf(
                "imageUrl" to p.imageUrl,
                "memberDisplayName" to (p.memberDisplayName ?: p.memberUsername),
                "memberAvatarUrl" to (p.memberAvatarUrl ?: ""),
                "likeCount" to p.likeCount,
                "discourseTopicUrl" to p.discourseTopicUrl,
            )
          })
    } catch (e: Exception) {
      log.warn("Failed to fetch featured projects for member: $requestedUsername", e)
      call.respond(emptyList<Map<String, Any>>())
    }
  }
}
