package org.dallasmakerspace.server.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.server.common.logging.LoggerFactory

@Singleton
class DMSHttpClient @Inject constructor(loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)

  private fun getClient() =
      HttpClient(CIO) {
        engine { endpoint { keepAliveTime = 0 } }
        install(ContentNegotiation) { jackson {} }
        install(Logging) {
          logger = Logger.DEFAULT
          level = LogLevel.INFO
        }
      }

  suspend fun get(url: String, customHeaders: StringValues): Map<String, Any> {
    val resp = getClient().use { it.get(url) { headers { appendAll(customHeaders) } } }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      // If member-profile-service encounters an error, it will return json with a message attribute
      val body = resp.body<String>()
      log.error("get: HTTP request failed: $body")
      val message =
          if (body.contains("message") && body.contains("ERROR")) {
            // Serialize the json to a map and extract the message attribute
            val json = resp.body<Map<String, Any>>()
            json["message"] as String
          } else ""
      throw HttpException(message)
    }
  }

  suspend fun patch(url: String, authHeaders: StringValues, payload: Any) {
    val resp =
        getClient().use {
          it.patch {
            url(url)
            headers { appendAll(authHeaders) }
            contentType(ContentType.Application.Json)
            setBody(payload)
          }
        }
    if (!resp.status.isSuccess()) {
      log.error("patch: HTTP request failed: $resp")
      throw HttpException("Failed to patch $url; Status: ${resp.status} FullResponse: $resp")
    }
  }

  suspend fun delete(url: String, authHeaders: StringValues, payload: Any) {
    val resp =
        getClient().use {
          it.delete {
            url(url)
            headers { appendAll(authHeaders) }
            contentType(ContentType.Application.Json)
            setBody(payload)
          }
        }
    if (!resp.status.isSuccess()) {
      log.error("delete: HTTP request failed: $resp")
      throw HttpException("Failed to delete $url; Status: ${resp.status} FullResponse: $resp")
    }
  }
}
