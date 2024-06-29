package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import org.dallasmakerspace.thymeleaf.server.auth.getOAuthSettings

fun Application.configureStatusPages() {
  val config = ApplicationConfig(null)
  val settings = getOAuthSettings(config)
  val ssoProfileUrl = settings.ssoProfileUrl
  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respond(
          status = status,
          ThymeleafContent("error404", mapOf("message" to "Sorry! Page was not found.")))
    }
    exception<Throwable> { call, cause ->
      call.respond(
          status = HttpStatusCode.InternalServerError,
          ThymeleafContent(
              "error5xx",
              mapOf(
                  "message" to "There was an error processing your request.",
                  "cause" to "$cause: ${cause.stackTraceToString()}",
                  "sso_profile_url" to ssoProfileUrl)))
    }
  }
}
