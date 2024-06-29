package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.config.*
import java.util.*
import javax.inject.Inject
import kotlin.random.Random
import org.dallasmakerspace.thymeleaf.server.common.AppConfig

private const val NONCE_RANGE_START = 1000000
private const val NONCE_RANGE_END = 9999999

class DiscourseSSOProvider
@Inject
constructor(private val discourseUtil: DiscourseUtil, private val appConfig: AppConfig) {

  fun getDiscourseSSOConnectUrl(): String {
    val (urlEncodedBase64Payload, signature) = getPayloadAndSignature()
    return "https://talk.dallasmakerspace.org/session/sso_provider?sso=$urlEncodedBase64Payload&sig=$signature"
  }

  private fun getPayloadAndSignature(): Pair<String, String> {
    val redirectUrl = appConfig.requireStringProperty("app.discourse.redirect-url")
    val randomNonce = Random.nextInt(NONCE_RANGE_START, NONCE_RANGE_END).toString()
    val payload = "nonce=$randomNonce&return_sso_url=$redirectUrl"
    val base64Payload = Base64.getEncoder().encodeToString(payload.toByteArray())
    val signature = discourseUtil.getHexSignature(base64Payload)
    val urlEncodedBase64Payload = java.net.URLEncoder.encode(base64Payload, "UTF-8")
    return Pair(urlEncodedBase64Payload, signature)
  }
}
