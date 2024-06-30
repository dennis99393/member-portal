package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.plugins.UserSession
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler

/**
 * Handles the callback from Discourse after a user has linked their account and decodes the SSO
 * payload.
 * - Compute the HMAC-SHA256 of sso using sso provider secret as your key.
 * - Convert sig from its hex string representation back into bytes.
 * - Make sure the above two values are equal.
 * - Base64 decode sso; you?ll get the passed embedded query string. This will have a key called
 *   nonce whose value should match the nonce passed originally. Make sure that this is the case,
 *   and be sure to delete the nonce from your system.
 * - You?ll find this query string will also contain a bunch of user information. Use as you see
 *   fit.
 *
 * Ref:
 * https://meta.discourse.org/t/use-discourse-as-an-identity-provider-sso-discourseconnect/32974
 */
class DiscourseCallbackHandler
@Inject
constructor(
    private val discourseUtil: DiscourseUtil,
    private val memberService: MemberService,
    private val userInfoProvider: UserInfoProvider,
    private val discourseNonceCache: DiscourseNonceCache
) : IRouteHandler {

  override suspend fun handle(call: ApplicationCall) {
    // Get sso and sig from query parameters
    val ssoStr = call.request.queryParameters["sso"].toString()
    val sigStr = call.request.queryParameters["sig"].toString()
    val failedParam = call.request.queryParameters["failed"]

    // Calculate the HMAC-SHA256 of sso using sso provider secret as your key
    val hmacSha256 = discourseUtil.getHexSignature(ssoStr)

    if (failedParam != null) {
      throw DiscourseException("Talk authentication failed")
    } else if (hmacSha256 != sigStr) {
      throw DiscourseException("Invalid signature")
    } else {
      // Url decode the sso payload
      val sso = withContext(Dispatchers.IO) { URLDecoder.decode(ssoStr, "UTF-8") }
      // Base64 decode the sso payload
      val ssoDecoded = java.util.Base64.getDecoder().decode(sso).decodeToString()
      val ssoMap =
          ssoDecoded.split("&").associate {
            val (key, value) = it.split("=")
            key to value
          }

      val session = call.sessions.get<UserSession>()
      val accessToken = session?.accessToken
      if (accessToken != null) {
        val jsonMap: Map<String, Any>?
        val username =
            try {
              jsonMap = userInfoProvider.getUserInfo(accessToken).toMutableMap()
              checkNotNull(jsonMap["preferred_username"] as String?) { "No username in userinfo" }
            } catch (e: AuthException) {
              call.sessions.clear<UserSession>()
              Log.e("Failed to get user info", e)
              throw DiscourseException("Failed to get user info")
            }

        // Get the nonce from the sso payload
        val nonce = ssoMap["nonce"] ?: throw DiscourseException("Missing nonce")

        // Get the original nonce from cache
        val originalNonce =
            discourseNonceCache.getNonce(username)
                ?: throw DiscourseException("Original nonce not found")

        if (nonce != originalNonce) {
          throw DiscourseException("Nonce returned by Discourse does not match original")
        }

        // Delete the nonce from cache
        discourseNonceCache.deleteNonce(username)

        val discourseUsername =
            ssoMap["username"] ?: throw DiscourseException("Missing discourse username")
        val discourseAvatarUrlUrlEncoded = ssoMap["avatar_url"]
        // URL decode the avatar URL
        val discourseAvatarUrl =
            withContext(Dispatchers.IO) { URLDecoder.decode(discourseAvatarUrlUrlEncoded, "UTF-8") }

        memberService.linkDiscourseAccount(username, discourseUsername, discourseAvatarUrl)

        // Redirect back to profile page
        call.respondRedirect("/profile/@$username", permanent = false)
      }
      throw DiscourseException("Access token is empty")
    }
  }
}
