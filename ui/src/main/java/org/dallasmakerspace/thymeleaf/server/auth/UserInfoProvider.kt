package org.dallasmakerspace.thymeleaf.server.auth

import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.common.AppConfig
import org.dallasmakerspace.thymeleaf.server.common.DMSHttpClient

class UserInfoProvider
@Inject
constructor(private val httpClient: DMSHttpClient, private val appConfig: AppConfig) {
  suspend fun getUserInfo(accessToken: String): Map<String, Any> {
    val resp =
        httpClient.get(appConfig.requireStringProperty("app.oidc.user-info-url"), accessToken)
    return resp
  }
}
