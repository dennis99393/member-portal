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

@Singleton
class DMSHttpClient @Inject constructor() {
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
      }

  suspend fun get(url: String, customHeaders: StringValues): Map<String, Any> {
    val resp = client.get(url) { headers { appendAll(customHeaders) } }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      Log.e("HTTP request failed: $resp", null)
      throw HttpException("Failed to get $url: ${resp.status}")
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
      Log.e("HTTP request failed: $resp", null)
      throw HttpException("Failed to patch $url: ${resp.status}")
    }
  }
}
