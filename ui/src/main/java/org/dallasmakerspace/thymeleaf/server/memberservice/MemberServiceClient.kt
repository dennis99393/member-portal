package org.dallasmakerspace.thymeleaf.server.memberservice

import io.ktor.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.common.AppConfig
import org.dallasmakerspace.thymeleaf.server.common.DMSHttpClient
import org.dallasmakerspace.thymeleaf.server.common.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.models.DMSMember

@Singleton
class MemberServiceClient
@Inject
constructor(
    loggerFactory: LoggerFactory,
    appConfig: AppConfig,
    private val dmsHttpClient: DMSHttpClient
) {
  private val log = loggerFactory.create(javaClass)
  private val baseUrl = appConfig.requireStringProperty("app.member-service.url")
  private val authHeaders =
      mapOf(
          "X-Api-Key" to appConfig.requireStringProperty("app.member-service.api-key"),
          "X-Api-Client" to appConfig.requireStringProperty("app.member-service.api-client"))

  suspend fun getMember(username: String, sessionId: String?): DMSMember {
    val userMap =
        try {
          val apiHeaders = getApiHeaders(sessionId)
          dmsHttpClient.get("$baseUrl/members/$username", apiHeaders)
        } catch (ignored: Exception) {
          log.error("Failed to get member: $username", ignored)
          throw MemberServiceException("Failed to get member: $username", ignored)
        }
    return DMSMember.fromMap(userMap)
  }

  suspend fun patchMember(username: String, member: DMSMember, sessionId: String?) {
    val apiHeaders = getApiHeaders(sessionId)
    val userMap = DMSMember.toMap(member)
    dmsHttpClient.patch("$baseUrl/members/$username/update", apiHeaders, userMap)
  }

  private fun getApiHeaders(sessionId: String?) =
      StringValues.build {
        sessionId?.apply { append("X-Request-Id", sessionId) }
        authHeaders.forEach(this::append)
      }
}
