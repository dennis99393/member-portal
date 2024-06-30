package org.dallasmakerspace.thymeleaf.server.discourse

import java.util.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.common.AppConfig

class DiscourseSSOProvider
@Inject
constructor(private val discourseUtil: DiscourseUtil, private val appConfig: AppConfig) {

  fun getDiscourseSSOConnectUrl(randomNonce: String): String {
    val (urlEncodedBase64Payload, signature) = getPayloadAndSignature(randomNonce)
    return "https://talk.dallasmakerspace.org/session/sso_provider?sso=$urlEncodedBase64Payload&sig=$signature"
  }

  private fun getPayloadAndSignature(randomNonce: String): Pair<String, String> {
    val redirectUrl = appConfig.requireStringProperty("app.discourse.redirect-url")
    val payload = "nonce=$randomNonce&return_sso_url=$redirectUrl"
    val base64Payload = Base64.getEncoder().encodeToString(payload.toByteArray())
    val signature = discourseUtil.getHexSignature(base64Payload)
    val urlEncodedBase64Payload = java.net.URLEncoder.encode(base64Payload, "UTF-8")
    return Pair(urlEncodedBase64Payload, signature)
  }
}
