package org.dallasmakerspace.server.linkedin

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple cache for storing state values for LinkedIn OAuth2. We store the random state generated
 * in [LinkLinkedInHandler] and validate it in [LinkedInCallbackHandler]. The state parameter is
 * used for CSRF protection in OAuth2 flows.
 */
@Singleton
class LinkedInStateCache @Inject constructor() {

  fun getState(key: String): LinkedInOAuthState? {
    return stateCache[key]
  }

  fun setState(key: String, state: LinkedInOAuthState) {
    stateCache[key] = state
  }

  fun deleteState(username: String) {
    stateCache.remove(username)
  }

  companion object {
    private val stateCache = mutableMapOf<String, LinkedInOAuthState>()
  }
}
