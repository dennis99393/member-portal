package org.dallasmakerspace.server.common

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.util.*
import org.dallasmakerspace.server.common.logging.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DMSHttpClient @Inject constructor(loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)
  private val objectMapper = jacksonObjectMapper()

  private fun getClient() =
      HttpClient(CIO) {
        engine {
          endpoint {
            keepAliveTime = 10_000
            maxConnectionsPerRoute = 100
            requestTimeout = 120_000 // 120 seconds for long-running reports
            connectTimeout = 10_000
            connectAttempts = 2
          }
        }
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
          try {
            if (body.contains("message")) {
              // Try to parse as JSON and extract the message attribute
              val json = objectMapper.readValue(body, Map::class.java)
              (json["message"] as? String) ?: "Status: ${resp.status}, Response: $body"
            } else {
              "Status: ${resp.status}, Response: $body"
            }
          } catch (e: JsonProcessingException) {
            log.warn("get: Failed to parse error response as JSON: ${e.message}")
            "Status: ${resp.status}, Response: $body"
          }
      throw HttpException(message)
    }
  }

  suspend fun post(url: String, authHeaders: StringValues, payload: Any): Map<String, Any> {
    val resp =
        getClient().use {
          it.post {
            url(url)
            headers { appendAll(authHeaders) }
            contentType(ContentType.Application.Json)
            setBody(payload)
          }
        }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      val body = resp.body<String>()
      log.error("post: HTTP request failed: $body")
      throw HttpException("Failed to post $url; Status: ${resp.status} Response: $body")
    }
  }

  suspend fun patch(url: String, authHeaders: StringValues, payload: Any): Map<String, Any> {
    val resp =
        getClient().use {
          it.patch {
            url(url)
            headers { appendAll(authHeaders) }
            contentType(ContentType.Application.Json)
            setBody(payload)
          }
        }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      val body = resp.body<String>()
      log.error("patch: HTTP request failed: $body")
      throw HttpException("Failed to patch $url; Status: ${resp.status} Response: $body")
    }
  }

  suspend fun delete(url: String, authHeaders: StringValues): Map<String, Any> {
    val resp =
        getClient().use {
          it.delete {
            url(url)
            headers { appendAll(authHeaders) }
          }
        }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      val body = resp.body<String>()
      log.error("delete: HTTP request failed: $body")
      throw HttpException("Failed to delete $url; Status: ${resp.status} Response: $body")
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
