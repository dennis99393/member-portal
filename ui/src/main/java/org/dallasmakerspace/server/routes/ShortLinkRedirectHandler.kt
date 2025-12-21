package org.dallasmakerspace.server.routes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.dallasmakerspace.server.common.logging.LoggerFactory

class ShortLinkRedirectHandler(
    loggerFactory: LoggerFactory,
    private val serviceBaseUrl: String,
) : IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
    log.info("Resolving short link: $path")

    val client = HttpClient(CIO) { followRedirects = false }

    try {
      val response = client.get("$serviceBaseUrl/go/$path")

      when (response.status) {
        HttpStatusCode.Found,
        HttpStatusCode.MovedPermanently,
        HttpStatusCode.TemporaryRedirect -> {
          val location = response.headers[HttpHeaders.Location]
          if (location != null) {
            log.info("Redirecting to: $location")
            call.respondRedirect(location, permanent = false)
          } else {
            log.error("Redirect response missing Location header")
            call.respondText(
                "Redirect location not found",
                status = HttpStatusCode.InternalServerError,
            )
          }
        }
        HttpStatusCode.NotFound -> {
          log.warn("Short link not found: $path")
          call.respondText("Short link not found: $path", status = HttpStatusCode.NotFound)
        }
        else -> {
          log.error("Unexpected response from service: ${response.status}")
          call.respondText("Unexpected response: ${response.status}", status = response.status)
        }
      }
    } catch (e: Exception) {
      log.error("Error resolving short link: $path", e)
      call.respondText(
          "Error resolving short link: ${e.message}",
          status = HttpStatusCode.InternalServerError,
      )
    } finally {
      client.close()
    }
  }
}
