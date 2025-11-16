package org.dallasmakerspace.server.discourse

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple cache for storing nonce values for Discourse SSO. We store the random nonce generated
 * for discourse in [DiscourseSSOProvider] We need to validate the nonce returned from Discourse in
 * [DiscourseCallbackHandler].
 */
@Singleton
class DiscourseNonceCache @Inject constructor() {

  fun getNonce(key: String): String? {
    return nonceCache[key]
  }

  fun setNonce(key: String, nonce: String) {
    nonceCache[key] = nonce
  }

  fun deleteNonce(username: String) {
    nonceCache.remove(username)
  }

  companion object {
    private val nonceCache = mutableMapOf<String, String>()
  }
}
