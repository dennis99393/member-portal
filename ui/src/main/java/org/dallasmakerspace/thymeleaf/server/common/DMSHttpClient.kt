package org.dallasmakerspace.thymeleaf.server.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory

@Singleton
class DMSHttpClient @Inject constructor(loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)
  @Suppress("MagicNumber")
  val client =
      HttpClient(CIO) {
        install(ContentNegotiation) {
          gson {
            setPrettyPrinting()
            setLenient()
          }
        }
        install(Logging) {
          logger = Logger.DEFAULT
          level = LogLevel.INFO
        }
        engine {
          maxConnectionsCount = 1000
          endpoint {
            connectTimeout = 100
            requestTimeout = 2000
            keepAliveTime = 5000
            pipelineMaxSize = 20
            maxConnectionsPerRoute = 100
          }
        }
      }

  suspend fun get(url: String, customHeaders: StringValues): Map<String, Any> {
    val resp = client.get(url) { headers { appendAll(customHeaders) } }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      log.error("get: HTTP request failed: $resp")
      throw HttpException("Failed to get $url; Status: ${resp.status} FullResponse: $resp")
    }
  }

  suspend fun patch(url: String, authHeaders: StringValues, payload: Map<String, Any?>) {
    val resp =
        client.patch {
          url(url)
          headers { appendAll(authHeaders) }
          contentType(ContentType.Application.Json)
          setBody(payload)
        }
    if (!resp.status.isSuccess()) {
      log.error("patch: HTTP request failed: $resp")
      throw HttpException("Failed to patch $url; Status: ${resp.status} FullResponse: $resp")
    }
  }
}
