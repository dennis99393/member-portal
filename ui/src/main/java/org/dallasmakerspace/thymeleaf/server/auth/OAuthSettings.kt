package org.dallasmakerspace.thymeleaf.server.auth

import org.dallasmakerspace.thymeleaf.server.common.AppConfig

data class OAuthSettings(
    val baseUrl: String,
    val authorizeUrl: String,
    val accessTokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val ssoProfileUrl: String
)

fun getOAuthSettings(appConfig: AppConfig): OAuthSettings {
  val baseUrl = appConfig.requireStringProperty("app.base-url")
  val authorizeUrl = appConfig.requireStringProperty("app.oidc.authorize-url")
  val accessTokenUrl = appConfig.requireStringProperty("app.oidc.access-token-url")
  val clientId = appConfig.requireStringProperty("app.oidc.client-id")
  val clientSecret = appConfig.requireStringProperty("app.oidc.client-secret")
  val profileUrl = appConfig.requireStringProperty("app.oidc.sso-profile-url")

  return OAuthSettings(baseUrl, authorizeUrl, accessTokenUrl, clientId, clientSecret, profileUrl)
}
