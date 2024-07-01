package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.webjars.*
import org.dallasmakerspace.thymeleaf.server.auth.OAuthSettings
import org.dallasmakerspace.thymeleaf.server.auth.getOAuthSettings
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent
import org.dallasmakerspace.thymeleaf.server.routes.RouteFactory

fun Application.configureHttp() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  val settings = getOAuthSettings(appConfig)
  val httpClient = getHttpClient()

  install(Compression) {
    gzip()
    deflate()
  }
  install(Webjars) { path = "assets" }
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
      onStateCreated = { call, state ->
        // saves new state with redirect url value
        call.request.queryParameters["redirectUrl"]?.let { RouteFactory.redirects[state] = it }
      })
}

data class UserSession(
    var accessToken: String? = null,
    var idHint: String? = null,
    var adPrincipalUser: Map<String, Any> = mapOf()
) : Principal
