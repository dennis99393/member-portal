package org.dallasmakerspace.server.memberservice

import io.ktor.util.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.DMSHttpClient
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.models.DMSMember
import org.dallasmakerspace.server.models.SearchPreloadResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Suppress("TooGenericExceptionCaught")
@Singleton
class MemberServiceClient
@Inject
constructor(
    loggerFactory: LoggerFactory,
    appConfig: AppConfig,
    private val dmsHttpClient: DMSHttpClient,
) {
  private val log = loggerFactory.create(javaClass)
  private val baseUrl = appConfig.requireStringProperty("app.member-service.url")
  private val authHeaders =
      mapOf(
          "X-Api-Key" to appConfig.requireStringProperty("app.member-service.api-key"),
          "X-Api-Client" to appConfig.requireStringProperty("app.member-service.api-client"),
      )

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

  suspend fun getSearchPreloads(sessionId: String?): SearchPreloadResponse = coroutineScope {
    val overallTimeoutMs = 15_000L

    withTimeout(overallTimeoutMs) {
      val apiHeaders = getApiHeaders(sessionId)

      // Fetch members then groups with per-request timeouts
      val members =
          try {
            withTimeout(12_000L) {
              val userMap = dmsHttpClient.get("$baseUrl/members", apiHeaders)
              val data =
                  userMap["data"] as List<*>?
                      ?: throw MemberServiceException("data attribute missing required")
              data.map { DMSMember.fromMap(it as Map<String, Any?>) }
            }
          } catch (ex: TimeoutCancellationException) {
            log.error("Timed out getting search preloads - members", ex)
            throw MemberServiceException("Timed out getting search preloads - members", ex)
          } catch (ex: CancellationException) {
            throw ex
          } catch (ex: Exception) {
            log.error("Failed to get search preloads - members", ex)
            throw MemberServiceException("Failed to get search preloads - members", ex)
          }

      val groups =
          try {
            withTimeout(12_000L) {
              val groupMap = dmsHttpClient.get("$baseUrl/groups", apiHeaders)
              val data =
                  groupMap["data"] as List<*>?
                      ?: throw MemberServiceException("data attribute missing required")
              data.map { DMSGroup.fromMap(it as Map<String, Any?>) }
            }
          } catch (ex: TimeoutCancellationException) {
            log.error("Timed out getting search preloads - groups", ex)
            throw MemberServiceException("Timed out getting search preloads - groups", ex)
          } catch (ex: CancellationException) {
            throw ex
          } catch (ex: Exception) {
            log.error("Failed to get search preloads - groups", ex)
            throw MemberServiceException("Failed to get search preloads - groups", ex)
          }

      SearchPreloadResponse(members, groups)
    }
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

  /**
   * Call the backend API with the given path, sessionId, and username. This is just a proxy for the
   * backend API, it will just pass the parameters through and return the response as a String.
   */
  suspend fun callBackendApi(path: String, sessionId: String?): Map<String, Any> {
    try {
      val apiHeaders = getApiHeaders(sessionId)
      val result = dmsHttpClient.get("$baseUrl/$path", apiHeaders)
      return result
    } catch (ex: Exception) {
      log.error("Failed to call backend API: $path ", ex)
      throw MemberServiceException("Failed to call backend API: $path", ex)
    }
  }
}
