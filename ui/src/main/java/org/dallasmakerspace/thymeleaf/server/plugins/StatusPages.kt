package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.sessions.*
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
      val session = call.sessions.get<UserSession>()
      val sessionId: String = session?.sessionId ?: "NO_SESSION"
      val userId: String = session?.userId ?: "NO_USER"
      call.respond(
          status = HttpStatusCode.InternalServerError,
          ThymeleafContent(
              "error5xx",
              mapOf(
                  "message" to "There was an error processing your request. ${cause.message}.",
                  "cause" to "$cause: ${cause.cause?.message}\n ${cause.stackTraceToString()}",
                  "sessionid" to sessionId,
                  "userid" to userId,
                  "sso_profile_url" to ssoProfileUrl)))
    }
  }
}
