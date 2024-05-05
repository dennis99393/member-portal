package org.dallasmakerspace.thymeleaf.server.auth

import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.common.DMSHttpClient

class UserInfoProvider @Inject constructor(private val httpClient: DMSHttpClient) {
  suspend fun getUserInfo(accessToken: String): Map<String, Any> {
    val resp =
        httpClient.get(
            "http://localhost:8080/realms/DMS/protocol/openid-connect/userinfo", accessToken)
    return resp
  }
}
