package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler

/**
 * Handles the callback from Discourse after a user has linked their account and decodes the SSO
 * payload.
 * - Compute the HMAC-SHA256 of sso using sso provider secret as your key.
 * - Convert sig from its hex string representation back into bytes.
 * - Make sure the above two values are equal.
 * - Base64 decode sso; you�ll get the passed embedded query string. This will have a key called
 *   nonce whose value should match the nonce passed originally. Make sure that this is the case,
 *   and be sure to delete the nonce from your system.
 * - You�ll find this query string will also contain a bunch of user information. Use as you see
 *   fit.
 *
 * Ref:
 * https://meta.discourse.org/t/use-discourse-as-an-identity-provider-sso-discourseconnect/32974
 */
class DiscourseCallbackHandler @Inject constructor(private val discourseUtil: DiscourseUtil) :
    IRouteHandler {

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

      // Get the nonce from the sso payload
      val nonce = ssoMap["nonce"] ?: throw DiscourseException("Missing nonce")

      val discourseUsername = ssoMap["username"] ?: throw DiscourseException("Missing username")

      // Delete the nonce from your system
      // Use the user information as you see fit
      call.respondText("Successfully linked account $discourseUsername", status = HttpStatusCode.OK)
    }
  }
}
