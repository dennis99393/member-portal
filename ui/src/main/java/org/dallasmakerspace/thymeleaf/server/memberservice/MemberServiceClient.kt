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
    private val appConfig: AppConfig,
    private val dmsHttpClient: DMSHttpClient
) {
  private val log = loggerFactory.create(javaClass)
  private val baseUrl = appConfig.requireStringProperty("app.member-service.url")
  private val authHeaders =
      StringValues.build {
        append("X-Api-Key", appConfig.requireStringProperty("app.member-service.api-key"))
        append("X-Api-Client", appConfig.requireStringProperty("app.member-service.api-client"))
      }

  suspend fun getMember(username: String): DMSMember {
    val userMap =
        try {
          dmsHttpClient.get("$baseUrl/members/$username", authHeaders)
        } catch (e: Exception) {
          log.error("Failed to get member: $username", e)
          throw MemberServiceException("Failed to get member: $username", e)
        }
    return DMSMember.fromMap(userMap)
  }

  suspend fun patchMember(username: String, member: DMSMember) {
    val userMap = DMSMember.toMap(member)
    dmsHttpClient.patch("$baseUrl/members/$username/update", authHeaders, userMap)
  }
}
