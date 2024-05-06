package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession

class ProfileHandler @Inject constructor(private val userInfoProvider: UserInfoProvider) :
    IRouteHandler {
  override suspend fun handle(call: ApplicationCall) {
    // Get username requested from path /profile/~{preferred_username}
    val usernameRequested = call.parameters["preferred_username"]
    val session = call.sessions.get<UserSession>()
    val accessToken = session?.accessToken
    if (accessToken != null) {
      val jsonMap: Map<String, Any>?

      try {
        jsonMap = userInfoProvider.getUserInfo(accessToken).toMutableMap()
        jsonMap["join_date"] = "Apr 2022"
        jsonMap["membership_duration"] = "2.2 years"
        /*
        TODO: Set talk username from API response
        jsonMap["talk_username"] =
        */
        val username = jsonMap["preferred_username"] as String?
        if (usernameRequested != username) {
          call.respond(
              ThymeleafContent(
                  "profile-private", mapOf("preferred_username" to "$usernameRequested")))
        } else {
          call.respond(ThymeleafContent("profile", jsonMap))
        }
      } catch (e: AuthException) {
        call.sessions.clear<UserSession>()
        Log.e("Failed to get user info", e)
        call.respondRedirect(RouteFactory.Paths.LOGIN.path, permanent = false)
      }
    } else {
      val currentUri = call.request.uri
      val encodedUri =
          withContext(Dispatchers.IO) {
            URLEncoder.encode(currentUri, StandardCharsets.UTF_8.toString())
          }
      call.respondRedirect(
          "${RouteFactory.Paths.LOGIN.path}?redirectUrl=$encodedUri", permanent = false)
    }
  }
}
