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

  val ALL: List<ConfigKey<*>> =
      listOf(
          VOTING_ELECTIONS_URL,
      )
}
