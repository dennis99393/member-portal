package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.util.concurrent.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.Permission
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.slug

class SearchPreloadHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handleAuthenticated(call: ApplicationCall) {

    log.debug("SearchPreloadHandler start")

    val cacheKey = "searchPreload"
    val currentTime = System.currentTimeMillis()

    val cachedData = cache[cacheKey]
    if (cachedData != null && currentTime - cachedData.first < cacheDuration) {
      log.debug("Returning cached data")
      call.respond(cachedData.second)
      return
    }

    val jsonMap: MutableMap<String, Any> = mutableMapOf()
    try {
      val preloadResponse = memberService.getSearchPreload(session.sessionId)

      // Process members
      val memberData =
          preloadResponse.members.map {
            val avatarUrl =
                it.discourseAvatarUrl
                    ?.takeIf { url -> url.isNotEmpty() }
                    ?.let { url ->
                      when {
                        url.startsWith("http") -> url
                        else -> "https://talk.dallasmakerspace.org$url"
                      }
                    }
            val record =
                mutableMapOf(
                    "displayName" to it.displayName,
                    "username" to it.username,
                    "discourseUsername" to it.discourseUsername,
                    "avatarUrl" to avatarUrl,
                    "type" to "member")
            if (authz.can(Permission.MANAGE_MEMBERS.name)) {
              record["badgeNumber"] = it.badgeNumber
              record["personalEmail"] = it.personalEmail
              record["phoneNumber"] = it.phoneNumber
            }
            record
          }

      // Process groups
      val groupData =
          preloadResponse.groups.map {
            mapOf(
                "displayName" to it.name,
                "username" to it.slug,
                "description" to it.description,
                "type" to "group")
          }

      // Combine members and groups
      val combinedData = memberData + groupData

      log.debug("SearchPreloadHandler members size: ${preloadResponse.members.size}")
      log.debug("SearchPreloadHandler groups size: ${preloadResponse.groups.size}")
      log.debug("SearchPreloadHandler combined size: ${combinedData.size}")

      jsonMap["status"] = "SUCCESS"
      jsonMap["data"] = combinedData

      cache[cacheKey] = Pair(currentTime, jsonMap)
      call.respond(jsonMap)
    } catch (e: Exception) {
      jsonMap["status"] = "ERROR"
      jsonMap["error"] = "${e.message}: ${e.stackTraceToString()}"
      call.respond(HttpStatusCode.InternalServerError, jsonMap)
    }
  }

  companion object {
    private val cache = ConcurrentHashMap<String, Pair<Long, Map<String, Any>>>()
    private val cacheDuration = TimeUnit.MINUTES.toMillis(5)
  }
}
