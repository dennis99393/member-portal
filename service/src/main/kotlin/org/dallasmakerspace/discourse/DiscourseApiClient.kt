package org.dallasmakerspace.discourse

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton
import okio.IOException
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

private const val DISCOURSE_BASE_URL = "https://talk.dallasmakerspace.org"

@Singleton
class DiscourseApiClient
@Inject
constructor(private val appConfig: AppConfig, loggerFactory: LoggerFactory) : IDiscourseApiClient {
  private val log = loggerFactory.create(javaClass)

  private fun getClient() =
      HttpClient(CIO) {
        install(Logging) {
          logger = Logger.DEFAULT
          level = LogLevel.BODY
        }
      }

  override suspend fun addUsersToGroup(
      memberUsernamesList: List<String>,
      groupId: DiscourseService.GroupId
  ) {
    performGroupOperation(memberUsernamesList, groupId, HttpMethod.Put) { response ->
      when (response.status) {
        HttpStatusCode.OK -> return@performGroupOperation
        HttpStatusCode.UnprocessableEntity -> {
          log.info("Member(s) $memberUsernamesList already exists in the group ${groupId.name}")
          return@performGroupOperation
        }
        else ->
            throw DiscourseApiException(
                "Failed to add members $memberUsernamesList to $groupId: ${response.status} - ${response.bodyAsText()}")
      }
    }
  }

  override suspend fun removeMembersFromGroup(
      memberUsernamesList: List<String>,
      groupId: DiscourseService.GroupId
  ) {
    performGroupOperation(memberUsernamesList, groupId, HttpMethod.Delete) { response ->
      if (response.status != HttpStatusCode.OK) {
        throw DiscourseApiException(
            "Failed to remove $memberUsernamesList from $groupId: ${response.status} - ${response.bodyAsText()}")
      }
    }
  }

  private suspend fun performDiscourseApiOperation(
      url: String,
      method: HttpMethod,
      jsonBody: String,
      apiKey: String,
      onResponse: suspend (HttpResponse) -> Unit
  ) {
    try {
      val response =
          getClient().use {
            it.request(url) {
              this.method = method
              header("Api-Key", apiKey)
              header("Api-Username", "system")
              contentType(ContentType.Application.Json)
              setBody(jsonBody)
            }
          }
      onResponse(response)
    } catch (e: IOException) {
      throw DiscourseApiException("Failed to perform Discourse API operation", e)
    }
  }

  private suspend fun performGroupOperation(
      memberUsernames: List<String>,
      groupId: DiscourseService.GroupId,
      method: HttpMethod,
      onResponse: suspend (HttpResponse) -> Unit
  ) {
    val baseUrl = DISCOURSE_BASE_URL
    val apiKey = appConfig.requireStringProperty("app.discourse.apiKey")
    val url = "$baseUrl/groups/${groupId.id}/members.json"
    // Generate a string from the list of usernames by joining with a comma
    val memberList = memberUsernames.joinToString(separator = ",")
    val json = """{"usernames": "$memberList"}"""

    performDiscourseApiOperation(url, method, json, apiKey, onResponse)
  }
}
