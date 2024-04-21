package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.oauth
import io.ktor.server.auth.session
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.http.content.staticFiles
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.getOrSet
import io.ktor.server.sessions.sessions
import io.ktor.server.thymeleaf.ThymeleafContent
import java.io.File
import org.dallasmakerspace.thymeleaf.data.DataHolder
import org.dallasmakerspace.thymeleaf.data.GradeValue
import org.dallasmakerspace.thymeleaf.server.auth.OAuthSettings
import org.dallasmakerspace.thymeleaf.server.auth.getOAuthSettings

fun Application.configureRouting() {
  val config = ApplicationConfig(null)
  val settings = getOAuthSettings(config)
  val httpClient = getHttpClient()
  val userSession = getUserSession()

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
      challenge { call.respondRedirect("/login", permanent = false) }
    }
  }

  routing {
    authenticate("DMS") {
      get("/login") { call.respondRedirect("/", permanent = false) }
      get("/oidc-callback") { handleOidcCallback(call, userSession) }
    }

    get("/") { handleRootGet(call, httpClient) }
    get("/profile") { handleProfileGet(call, httpClient) }
    get("/report-card/{id}") { handleReportCardGet(call) }
    post("/report-card/{id}") { handleReportCardPost(call) }
    staticFiles("/static", File("static"))
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

private fun getUserSession(): UserSession {
  return UserSession()
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

private suspend fun handleOidcCallback(call: ApplicationCall, userSession: UserSession) {
  val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
  if (principal == null) {
    call.respondRedirect("/login", permanent = false)
    return
  }
  val session = call.sessions.getOrSet { userSession }
  session.accessToken = principal.accessToken
  session.idHint = principal.extraParameters["id_token"]

  call.respondRedirect("/", permanent = false)
}

private suspend fun handleRootGet(call: ApplicationCall, httpClient: HttpClient) {
  val session = call.sessions.get<UserSession>()
  if (session?.accessToken != null) {
    val jsonMap = getUserInfo(httpClient, session)
    call.respond(ThymeleafContent("index", jsonMap))
    return
  }
  call.respondRedirect("/login", permanent = false)
}

private suspend fun handleProfileGet(call: ApplicationCall, httpClient: HttpClient) {
  val session = call.sessions.get<UserSession>()
  if (session?.accessToken == null) {
    call.respondRedirect("/login", permanent = false)
    return
  }
  call.respond(ThymeleafContent("profile", getUserInfo(httpClient, session)))
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

private suspend fun getUserInfo(httpClient: HttpClient, session: UserSession): Map<String, Any> {
  val resp =
      httpClient.get("http://localhost:8080/realms/DMS/protocol/openid-connect/userinfo") {
        headers { append(HttpHeaders.Authorization, "Bearer ${session.accessToken}") }
      }
  if (resp.status.isSuccess()) {
    return resp.body<Map<String, Any>>()
  }
  return mapOf()
}

data class UserSession(
    var accessToken: String? = null,
    var idHint: String? = null,
    var adPrincipalUser: Map<String, Any> = mapOf() // ADPrincipalUser? = null
) : Principal
