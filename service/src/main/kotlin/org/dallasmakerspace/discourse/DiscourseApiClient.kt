package org.dallasmakerspace.discourse

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton
import okio.IOException
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.Log

private const val DISCOURSE_BASE_URL = "https://talk.dallasmakerspace.org"

@Singleton
class DiscourseApiClient
@Inject
constructor(private val appConfig: AppConfig, private val log: Log) : IDiscourseApiClient {
  private val client =
      HttpClient(CIO) {
        install(Logging) {
          logger = Logger.DEFAULT
          level = LogLevel.BODY
        }
      }

  override suspend fun addUserToGroup(memberUsername: String, groupId: DiscourseService.GroupId) {
    performGroupOperation(memberUsername, groupId, HttpMethod.Put) { response ->
      when (response.status) {
        HttpStatusCode.OK -> return@performGroupOperation
        HttpStatusCode.UnprocessableEntity -> {
          log.i("Member $memberUsername already exists in the group ${groupId.name}")
          return@performGroupOperation
        }
        else ->
            throw DiscourseApiException(
                "Failed to add member $memberUsername to $groupId: ${response.status} - ${response.bodyAsText()}")
      }
    }
  }

  override suspend fun removeMemberFromGroup(
      memberUsername: String,
      groupId: DiscourseService.GroupId
  ) {
    performGroupOperation(memberUsername, groupId, HttpMethod.Delete) { response ->
      if (response.status != HttpStatusCode.OK) {
        throw DiscourseApiException(
            "Failed to remove $memberUsername from $groupId: ${response.status} - ${response.bodyAsText()}")
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
          client.request(url) {
            this.method = method
            header("Api-Key", apiKey)
            header("Api-Username", "system")
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
          }
      onResponse(response)
    } catch (e: IOException) {
      throw DiscourseApiException("Failed to perform Discourse API operation", e)
    }
  }

  private suspend fun performGroupOperation(
      memberUsername: String,
      groupId: DiscourseService.GroupId,
      method: HttpMethod,
      onResponse: suspend (HttpResponse) -> Unit
  ) {
    val baseUrl = DISCOURSE_BASE_URL
    val apiKey = appConfig.requireStringProperty("app.discourse.apiKey")
    val url = "$baseUrl/groups/${groupId.id}/members.json"
    val json = """{"usernames": "$memberUsername"}"""

    performDiscourseApiOperation(url, method, json, apiKey, onResponse)
  }
}
