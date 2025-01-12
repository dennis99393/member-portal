package org.dallasmakerspace.server.memberservice

import io.ktor.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.DMSHttpClient
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.models.DMSMember

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
    val data =
        userMap["data"] as Map<*, *>?
            ?: throw MemberServiceException("data attribute missing required")
    return DMSMember.fromMap(data as Map<String, Any?>)
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

  suspend fun getGroup(groupName: String, sessionId: String?): DMSGroup {
    val groupMap =
        try {
          val apiHeaders = getApiHeaders(sessionId)
          dmsHttpClient.get("$baseUrl/groups/$groupName", apiHeaders)
        } catch (ignored: Exception) {
          log.error("Failed to get group: $groupName", ignored)
          throw MemberServiceException("Failed to get group: $groupName", ignored)
        }
    val data =
        if (groupMap["data"] is Map<*, *>) groupMap["data"] as Map<String, Any?>
        else throw MemberServiceException("data attribute missing required")
    return DMSGroup.fromMap(data)
  }

  suspend fun getSearchPreloads(sessionId: String?): List<DMSMember> {
    val userMap =
        try {
          val apiHeaders = getApiHeaders(sessionId)
          dmsHttpClient.get("$baseUrl/members?loggedInDays=30", apiHeaders)
        } catch (ignored: Exception) {
          log.error("Failed to get search preloads", ignored)
          throw MemberServiceException("Failed to get search preloads", ignored)
        }
    val data =
        userMap["data"] as List<*>?
            ?: throw MemberServiceException("data attribute missing required")
    return data.map { DMSMember.fromMap(it as Map<String, Any?>) }
  }

  suspend fun addToGroup(sessionId: String?, username: String, groupName: String) {
    try {
      val apiHeaders = getApiHeaders(sessionId)
      dmsHttpClient.patch("$baseUrl/groups/$groupName/add", apiHeaders, username)
    } catch (ex: Exception) {
      log.error("Failed to add $username to group: $groupName", ex)
      throw MemberServiceException("Failed to add $username to group: $groupName", ex)
    }
  }

  suspend fun removeFromGroup(sessionId: String?, username: String, groupName: String) {
    try {
      val apiHeaders = getApiHeaders(sessionId)
      dmsHttpClient.delete("$baseUrl/groups/$groupName/add", apiHeaders, username)
    } catch (ex: Exception) {
      log.error("Failed to remove $username from group: $groupName", ex)
      throw MemberServiceException("Failed to remove $username from group: $groupName", ex)
    }
  }
}
