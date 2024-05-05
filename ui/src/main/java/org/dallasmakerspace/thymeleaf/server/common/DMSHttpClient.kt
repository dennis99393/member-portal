package org.dallasmakerspace.thymeleaf.server.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException

@Singleton
class DMSHttpClient @Inject constructor() {
  suspend fun get(url: String, accessToken: String): Map<String, Any> {
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
    require(accessToken.isNotEmpty()) { "Access token is null or empty" }
    val resp =
        client.get(url) { headers { append(HttpHeaders.Authorization, "Bearer $accessToken") } }
    if (resp.status.isSuccess()) {
      return resp.body<Map<String, Any>>()
    } else {
      Log.e("HTTP request failed: $resp", null)
      throw AuthException("Failed to get user info: ${resp.status}")
    }
  }
}
