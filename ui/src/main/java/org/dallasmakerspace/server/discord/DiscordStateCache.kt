package org.dallasmakerspace.server.discord

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A simple cache for storing state values for Discord OAuth2. We store the random state generated
 * in [LinkDiscordHandler] and validate it in [DiscordCallbackHandler]. The state parameter is used
 * for CSRF protection in OAuth2 flows.
 */
@Singleton
class DiscordStateCache @Inject constructor() {

  fun getState(key: String): String? {
    return stateCache[key]
  }

  fun setState(key: String, state: String) {
    stateCache[key] = state
  }

  fun deleteState(username: String) {
    stateCache.remove(username)
  }

  companion object {
    private val stateCache = mutableMapOf<String, String>()
  }
}
