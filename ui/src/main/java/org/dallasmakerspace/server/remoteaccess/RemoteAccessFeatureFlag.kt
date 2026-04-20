package org.dallasmakerspace.server.remoteaccess

object RemoteAccessFeatureFlag {
  @Volatile private var enabled: Boolean = false

  @JvmStatic fun isEnabled(): Boolean = enabled

  fun update(value: Boolean) {
    enabled = value
  }
}
