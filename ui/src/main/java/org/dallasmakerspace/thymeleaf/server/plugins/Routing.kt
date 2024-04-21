package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.io.File
import org.dallasmakerspace.thymeleaf.data.DataHolder
import org.dallasmakerspace.thymeleaf.data.GradeValue

fun Application.configureRouting() {
  install(Sessions) { cookie<UserSession>("user_session", SessionStorageMemory()) }
  val httpClient =
      HttpClient(CIO) {
        install(ContentNegotiation) {
          gson {
            setPrettyPrinting()
            setLenient()
          }
        }
      }
  install(Authentication) {
    oauth("DMS") {
      urlProvider = { "http://localhost:8000/oidc-callback" }
      providerLookup = {
        OAuthServerSettings.OAuth2ServerSettings(
            name = "DMS",
            authorizeUrl = "http://localhost:8080/realms/DMS/protocol/openid-connect/auth",
            accessTokenUrl = "http://localhost:8080/realms/DMS/protocol/openid-connect/token",
            requestMethod = HttpMethod.Post,
            clientId = "member-profile",
            clientSecret = "dummy-secret-for-dev-mode",
            defaultScopes = listOf("openid", "profile", "email"),
        )
      }
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

      get("/oidc-callback") {
        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
        if (principal == null) {
          call.respondRedirect("/login", permanent = false)
          return@get
        }
        val session = call.sessions.getOrSet { UserSession() }
        session.accessToken = principal.accessToken
        session.idHint = principal.extraParameters["id_token"]

        call.respondRedirect("/", permanent = false)
      }
    }

    get("/") {
      val session = call.sessions.get<UserSession>()

      if (session?.accessToken != null) {
        // Get the JSON response and convert it to a map needed for Thymeleaf
        val jsonMap = getUserInfo(httpClient, session)

        call.respond(ThymeleafContent("index", jsonMap ?: mapOf()))
        return@get
      }
      call.respondRedirect("/login", permanent = false)
    }

    get("/profile") {
      val session = call.sessions.get<UserSession>()
      if (session?.accessToken == null) {
        call.respondRedirect("/login", permanent = false)
        return@get
      }
      call.respond(ThymeleafContent("profile", getUserInfo(httpClient, session) ?: mapOf()))
    }

    get("/report-card/{id}") {
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
    post("/report-card/{id}") {
      val parameters = call.receiveParameters()
      DataHolder.updateGrades(call.parameters["id"], parameters)
      call.respondRedirect("/", false)
    }
    staticFiles("/static", File("static"))
  }
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
