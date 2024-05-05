package org.dallasmakerspace.thymeleaf.server.auth

import io.ktor.server.config.ApplicationConfig

data class OAuthSettings(
    val baseUrl: String,
    val authorizeUrl: String,
    val accessTokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val ssoProfileUrl: String
)

fun getOAuthSettings(config: ApplicationConfig): OAuthSettings {
  val baseUrl =
      requireNotNull(config.propertyOrNull("app.base-url")) { "base Url is not configured" }
          .getString()
  val authorizeUrl =
      requireNotNull(config.propertyOrNull("app.oidc.authorize-url")) {
            "authorize Url is not configured"
          }
          .getString()
  val accessTokenUrl =
      requireNotNull(config.propertyOrNull("app.oidc.access-token-url")) {
            "access token Url is not configured"
          }
          .getString()
  val clientId =
      requireNotNull(config.propertyOrNull("app.oidc.client-id")) { "client id is not configured" }
          .getString()
  val clientSecret =
      requireNotNull(config.propertyOrNull("app.oidc.client-secret")) {
            "client secret is not configured"
          }
          .getString()
  val profileUrl =
      requireNotNull(config.propertyOrNull("app.oidc.sso-profile-url")) {
            "sso profile url is not configured"
          }
          .getString()

  return OAuthSettings(baseUrl, authorizeUrl, accessTokenUrl, clientId, clientSecret, profileUrl)
}
