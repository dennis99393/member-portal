package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.util.concurrent.ConcurrentHashMap
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.AuthenticatedHandler

private const val FLAG_CACHE_TTL_MS = 60_000L

private data class CachedFlag(val value: Boolean, val expiresAt: Long)

private val flagCache = ConcurrentHashMap<String, CachedFlag>()

private suspend fun getCachedFlag(
    client: MemberServiceClient,
    key: String,
    sessionId: String?,
    isInfraUser: Boolean,
): Boolean {
  val cacheKey = "$key:$isInfraUser"
  val cached = flagCache[cacheKey]
  if (cached != null && System.currentTimeMillis() < cached.expiresAt) return cached.value
  val value = client.getFeatureFlag(key, sessionId, isInfraUser)
  flagCache[cacheKey] = CachedFlag(value, System.currentTimeMillis() + FLAG_CACHE_TTL_MS)
  return value
}

fun Application.configureCommonModel(memberServiceClient: MemberServiceClient) {
  val plugin =
      createApplicationPlugin("CommonModel") {
        onCallRespond { call ->
          transformBody { body ->
            if (body is ThymeleafContent) {
              val isInfraUser =
                  call.attributes.getOrNull(AuthenticatedHandler.IS_INFRA_USER_KEY) ?: false
              val sessionId = call.sessions.get<UserSession>()?.sessionId
              val remoteAccessEnabled =
                  getCachedFlag(
                      memberServiceClient, "remote-access.enabled", sessionId, isInfraUser)
              ThymeleafContent(
                  body.template,
                  body.model +
                      mapOf(
                          "isInfraUser" to isInfraUser,
                          "remoteAccessEnabled" to remoteAccessEnabled),
                  body.etag,
                  body.contentType,
                  body.locale,
              )
            } else body
          }
        }
      }
  install(plugin)
}
