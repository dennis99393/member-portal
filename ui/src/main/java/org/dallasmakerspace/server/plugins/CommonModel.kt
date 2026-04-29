package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.util.concurrent.ConcurrentHashMap
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.AuthenticatedHandler
import org.slf4j.LoggerFactory

private const val FLAG_CACHE_TTL_MS = 60_000L

private data class CachedFlag(val value: Boolean, val expiresAt: Long)

private val flagCache = ConcurrentHashMap<String, CachedFlag>()

private val cmLog = LoggerFactory.getLogger("CommonModel")

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
  cmLog.info("Feature flag $key (isInfraUser=$isInfraUser) resolved to $value")
  flagCache[cacheKey] = CachedFlag(value, System.currentTimeMillis() + FLAG_CACHE_TTL_MS)
  return value
}

fun Application.configureCommonModel(memberServiceClient: MemberServiceClient) {
  sendPipeline.intercept(ApplicationSendPipeline.Before) {
    val body = subject
    if (body !is ThymeleafContent) return@intercept
    val isInfraUser = call.attributes.getOrNull(AuthenticatedHandler.IS_INFRA_USER_KEY) ?: false
    val sessionId = call.sessions.get<UserSession>()?.sessionId
    cmLog.info(
        "CommonModel: template=${body.template} isInfraUser=$isInfraUser sessionId=$sessionId")
    val remoteAccessEnabled =
        getCachedFlag(memberServiceClient, "remote-access.enabled", sessionId, isInfraUser)
    cmLog.info("CommonModel: remoteAccessEnabled=$remoteAccessEnabled")
    val currentUsername = call.sessions.get<UserSession>()?.userId ?: ""
    proceedWith(
        ThymeleafContent(
            body.template,
            body.model +
                mapOf(
                    "isInfraUser" to isInfraUser,
                    "remoteAccessEnabled" to remoteAccessEnabled,
                    "current_username" to currentUsername,
                ),
            body.etag,
            body.contentType,
            body.locale,
            body.fragments,
        ))
  }
}
