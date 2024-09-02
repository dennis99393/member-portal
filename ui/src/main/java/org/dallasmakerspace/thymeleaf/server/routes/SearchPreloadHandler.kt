package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.util.concurrent.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService

class SearchPreloadHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handle(call: ApplicationCall) {
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
      val preloadList = memberService.getSearchPreload(session?.sessionId)
      // Print out list size
      log.debug("SearchPreloadHandler preloadList size: ${preloadList.size}")
      // Return list with only DMSMember.displayName and DMSMember.discordUserId
      jsonMap["status"] = "SUCCESS"
      jsonMap["data"] =
          preloadList.map {
            mapOf(
                "displayName" to it.displayName,
                "username" to it.username,
                "discourseUsername" to it.discourseUsername)
          }
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
