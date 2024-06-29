package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent
import org.dallasmakerspace.thymeleaf.server.auth.getOAuthSettings
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent

fun Application.configureStatusPages() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  val settings = getOAuthSettings(appConfig)
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
                  "cause" to "$cause:\n ${cause.stackTraceToString()}",
                  "sso_profile_url" to ssoProfileUrl)))
    }
  }
}
