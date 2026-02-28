package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient

class IndexHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) = coroutineScope {
    log.info("Handle request: ${call.request}")

    val featuredProjectsDeferred = async {
      memberServiceClient.getFeaturedProjects(session.sessionId)
    }

    val jsonMap: MutableMap<String, Any> = userInfo.toMutableMap()
    jsonMap["profile_url"] = "./profile/@${jsonMap["preferred_username"]}"

    val featuredProjects = featuredProjectsDeferred.await()
    if (featuredProjects.isNotEmpty()) {
      jsonMap["featured_projects"] =
          featuredProjects.map { project ->
            mapOf(
                "topicId" to project.topicId,
                "postId" to project.postId,
                "title" to project.title,
                "imageUrl" to project.imageUrl,
                "memberUsername" to project.memberUsername,
                "memberDisplayName" to (project.memberDisplayName ?: project.memberUsername),
                "memberAvatarUrl" to (project.memberAvatarUrl ?: ""),
                "likeCount" to project.likeCount,
                "discourseTopicUrl" to project.discourseTopicUrl,
                "profileUrl" to "/profile/@${project.memberUsername}",
            )
          }
    }

    call.respond(ThymeleafContent("index", jsonMap))
  }
}
