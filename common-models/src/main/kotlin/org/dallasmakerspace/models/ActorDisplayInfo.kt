package org.dallasmakerspace.models

/**
 * Represents the display information for an actor in group history events. Used to handle special
 * service accounts and provide appropriate display names and links.
 */
data class ActorDisplayInfo(
    val displayName: String,
    val link: String,
    val isExternal: Boolean,
)

/**
 * Utility object for resolving actor display information based on special service account rules.
 */
object ActorDisplayResolver {

  /**
   * Resolves the display information for an actor based on their username and the group context.
   *
   * Special cases handled:
   * - svc_makermanager3 in "Voting Members" group → shows "self" (member added themselves)
   * - svc_moodle → shows "DMS Learn" with external link
   * - svc_makermanager3 (other groups) → shows "DMS Calendar" with external link
   *
   * @param actorUsername The username of the actor
   * @param groupName The name of the group (used for context-specific handling)
   * @param fallbackDisplayName The display name to use if no special handling applies
   * @return ActorDisplayInfo with the resolved display name, link, and external flag
   */
  fun resolve(
      actorUsername: String,
      groupName: String,
      fallbackDisplayName: String? = null,
  ): ActorDisplayInfo {
    val username = actorUsername.lowercase()

    return when {
      // Special case: svc_makermanager3 in "Voting Members" group should show "Self"
      groupName.equals("Voting Members", ignoreCase = true) && username == "svc_makermanager3" ->
          ActorDisplayInfo(
              displayName = "Self",
              link = "#",
              isExternal = false,
          )
      // svc_moodle → DMS Learn
      username == "svc_moodle" ->
          ActorDisplayInfo(
              displayName = "DMS Learn",
              link = "https://learn.dallasmakerspace.org",
              isExternal = true,
          )
      // svc_makermanager3 (not in Voting Members) → DMS Calendar
      username == "svc_makermanager3" ->
          ActorDisplayInfo(
              displayName = "DMS Calendar",
              link = "https://calendar.dallasmakerspace.org",
              isExternal = true,
          )
      // Default case: use the provided display name and profile link
      else ->
          ActorDisplayInfo(
              displayName = fallbackDisplayName ?: actorUsername,
              link = "/profile/@$actorUsername",
              isExternal = false,
          )
    }
  }
}
