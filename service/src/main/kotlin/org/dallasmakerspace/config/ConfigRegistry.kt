package org.dallasmakerspace.config

object ConfigRegistry {
  val VOTING_ELECTIONS_URL =
      ConfigKey(
          key = "voting.elections-url",
          description = "URL for the DMS elections procedures information page",
          category = "Voting",
          defaultValue = "https://source.dallasmakerspace.org/display/Board/Board+of+Directors",
          type = ConfigValueType.StringType,
          validators = listOf(NotBlankValidator(), UrlValidator()),
      )

  val GROUP_MEMBER_MANAGEMENT_ENABLED =
      ConfigKey(
          key = "groups.member-management-enabled",
          description = "Enable adding and removing members from groups via the member portal",
          category = "Groups",
          defaultValue = false,
          type = ConfigValueType.BooleanType,
      )

  val ALL: List<ConfigKey<*>> =
      listOf(
          VOTING_ELECTIONS_URL,
          GROUP_MEMBER_MANAGEMENT_ENABLED,
      )
}
