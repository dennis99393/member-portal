package org.dallasmakerspace.server.remoteaccess

import java.util.Base64

object GuacamoleClientId {
  fun encode(connectionId: String): String {
    val raw = "$connectionId\u0000c\u0000postgresql"
    return Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
  }
}
