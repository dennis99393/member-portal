package org.dallasmakerspace.server.remoteaccess

import org.dallasmakerspace.server.memberservice.MemberServiceClient

private const val TTL_MS = 60_000L

object RemoteAccessFeatureFlag {
  @Volatile private var enabled: Boolean = false
  @Volatile private var refreshedAt: Long = 0L
  @Volatile private var client: MemberServiceClient? = null

  @JvmStatic fun isEnabled(): Boolean = enabled

  fun setClient(c: MemberServiceClient) {
    client = c
  }

  fun update(value: Boolean) {
    enabled = value
    refreshedAt = System.currentTimeMillis()
  }

  suspend fun refreshIfStale(sessionId: String?) {
    val c = client ?: return
    if (System.currentTimeMillis() - refreshedAt < TTL_MS) return
    val value = c.getFeatureFlag("remote-access.enabled", sessionId)
    update(value)
  }
}
