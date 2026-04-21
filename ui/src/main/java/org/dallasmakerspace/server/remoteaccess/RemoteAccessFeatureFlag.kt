package org.dallasmakerspace.server.remoteaccess

import org.dallasmakerspace.server.memberservice.MemberServiceClient

private const val TTL_MS = 60_000L

object RemoteAccessFeatureFlag {
  @Volatile private var enabledForInfra: Boolean = false
  @Volatile private var enabledForNonInfra: Boolean = false
  @Volatile private var infraRefreshedAt: Long = 0L
  @Volatile private var nonInfraRefreshedAt: Long = 0L
  @Volatile private var client: MemberServiceClient? = null

  @JvmStatic
  fun isEnabled(isInfraUser: Boolean): Boolean =
      if (isInfraUser) enabledForInfra else enabledForNonInfra

  fun setClient(c: MemberServiceClient) {
    client = c
  }

  fun update(value: Boolean, isInfraUser: Boolean) {
    if (isInfraUser) {
      enabledForInfra = value
      infraRefreshedAt = System.currentTimeMillis()
    } else {
      enabledForNonInfra = value
      nonInfraRefreshedAt = System.currentTimeMillis()
    }
  }

  suspend fun refreshIfStale(sessionId: String?, isInfraUser: Boolean) {
    val c = client ?: return
    val refreshedAt = if (isInfraUser) infraRefreshedAt else nonInfraRefreshedAt
    if (System.currentTimeMillis() - refreshedAt < TTL_MS) return
    val value = c.getFeatureFlag("remote-access.enabled", sessionId, isInfraUser)
    update(value, isInfraUser)
  }
}
