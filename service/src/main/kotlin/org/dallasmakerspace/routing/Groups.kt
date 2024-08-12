package org.dallasmakerspace.routing

import io.ktor.http.*
import io.ktor.resources.*

@Resource("/groups")
class Groups {

  @Resource("{groupslug}")
  class DMSGroup(
      val parent: Groups = Groups(),
      val groupslug: String,
      val members: List<String>? = null,
  )

  companion object {
    /**
     * Generate a group name from a slug (URL Fragment). The slug can have "+" that get translated
     * to spaces.
     */
    fun getNameFromSlug(slug: String) = slug.replace("+", " ").decodeURLPart()
  }
}
