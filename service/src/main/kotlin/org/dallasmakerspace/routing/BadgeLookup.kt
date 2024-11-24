package org.dallasmakerspace.routing

import io.ktor.resources.*

@Resource("/badge-lookup/{badgeNumber}")
class BadgeLookup {
  val badgeNumber: String = ""
}
