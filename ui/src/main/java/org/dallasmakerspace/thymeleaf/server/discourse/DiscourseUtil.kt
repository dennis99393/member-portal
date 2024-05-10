package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.config.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class DiscourseUtil @Inject constructor() {
  private val config = ApplicationConfig(null)

  fun getHexSignature(urlEncodedBase64Payload: String): String {
    // Generate a HMAC-SHA256 signature from BASE64_PAYLOAD using your sso provider secret as the
    // key, then create a lower case hex string from this
    // https://meta.discourse.org/t/use-discourse-as-an-identity-provider-sso-discourseconnect/32974
    val secret =
        requireNotNull(config.propertyOrNull("app.discourse.sso-secret")?.getString()) {
          "sso secret is not configured"
        }
    val mac = Mac.getInstance("HmacSHA256")
    val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.US_ASCII), "HmacSHA256")
    mac.init(secretKeySpec)
    val signatureBytes = mac.doFinal(urlEncodedBase64Payload.toByteArray(Charsets.US_ASCII))
    return signatureBytes.joinToString("") { String.format(Locale.US, "%02X", it) }.lowercase()
  }
}
