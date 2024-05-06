package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.io.File
import org.dallasmakerspace.thymeleaf.data.DataHolder
import org.dallasmakerspace.thymeleaf.data.GradeValue
import org.dallasmakerspace.thymeleaf.server.auth.OAuthSettings
import org.dallasmakerspace.thymeleaf.server.auth.getOAuthSettings
import org.dallasmakerspace.thymeleaf.server.routes.RouteFactory

fun Application.configureRouting() {
  val config = ApplicationConfig(null)
  val settings = getOAuthSettings(config)
  val httpClient = getHttpClient()

  install(Sessions) { cookie<UserSession>("user_session", SessionStorageMemory()) }
  install(Authentication) {
    oauth("DMS") {
      urlProvider = { "${settings.baseUrl}/oidc-callback" }
      providerLookup = { getOAuthServerSettings(settings) }
      client = httpClient
    }
    session<UserSession>("auth_session") {
      validate { session ->
        if (session.accessToken != null) {
          session
        } else {
          null
        }
      }
      challenge { call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false) }
    }
  }

  routing {
    authenticate("DMS") {
      get(RouteFactory.Paths.LOGIN.path) { RouteFactory.getHandler(call)?.handle(call) }
      get(RouteFactory.Paths.OIDC_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.OIDC_CALLBACK.path)?.handle(call)
      }
    }

    get(RouteFactory.Paths.INDEX.path) { RouteFactory.getHandler(call)?.handle(call) }
    get(RouteFactory.Paths.PROFILE.path) {
      RouteFactory.getHandler(call)?.handle(call)
    }
    get("/report-card/{id}") { handleReportCardGet(call) }
    post("/report-card/{id}") { handleReportCardPost(call) }
    staticFiles(RouteFactory.Paths.STATIC.path, File("static"))
  }
}

private fun getHttpClient(): HttpClient {
  return HttpClient(CIO) {
    install(ContentNegotiation) {
      gson {
        setPrettyPrinting()
        setLenient()
      }
    }
  }
}

private fun getOAuthServerSettings(
    settings: OAuthSettings
): OAuthServerSettings.OAuth2ServerSettings {
  return OAuthServerSettings.OAuth2ServerSettings(
      name = "DMS",
      authorizeUrl = settings.authorizeUrl,
      accessTokenUrl = settings.accessTokenUrl,
      requestMethod = HttpMethod.Post,
      clientId = settings.clientId,
      clientSecret = settings.clientSecret,
      defaultScopes = listOf("openid", "profile", "email"),
  )
}

private suspend fun handleReportCardGet(call: ApplicationCall) {
  call.respond(
      ThymeleafContent(
          "report-card",
          mapOf(
              "student" to DataHolder.findStudentById(call.parameters["id"]),
              "gradeOptionList" to GradeValue.entries,
          ),
      ),
  )
}

private suspend fun handleReportCardPost(call: ApplicationCall) {
  val parameters = call.receiveParameters()
  DataHolder.updateGrades(call.parameters["id"], parameters)
  call.respondRedirect("/", false)
}

data class UserSession(
    var accessToken: String? = null,
    var idHint: String? = null,
    var adPrincipalUser: Map<String, Any> = mapOf()
) : Principal
