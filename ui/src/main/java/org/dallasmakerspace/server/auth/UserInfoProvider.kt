package org.dallasmakerspace.server.auth

import io.ktor.http.*
import io.ktor.util.*
import javax.inject.Inject
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.DMSHttpClient

class UserInfoProvider
@Inject
constructor(private val httpClient: DMSHttpClient, private val appConfig: AppConfig) {
  suspend fun getUserInfo(accessToken: String): Map<String, Any> {
    val resp =
        httpClient.get(
            appConfig.requireStringProperty("app.oidc.user-info-url"),
            StringValues.build { append(HttpHeaders.Authorization, "Bearer $accessToken") })
    return resp
  }
}
