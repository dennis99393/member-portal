package org.dallasmakerspace.server.voterregistration

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toKotlinLocalDate
import org.dallasmakerspace.server.common.Time

@Singleton
class VoterRegistrationManager @Inject constructor(private val time: Time) {
  /** Returns the number of days since the last inactive date. */
  fun getDaysSinceLastInactiveDate(lastInactiveDate: LocalDate): Int {
    val today = time.getToday()
    return lastInactiveDate.daysUntil(today.toKotlinLocalDate())
  }

  /**
   * Returns a string representation of the number of days since the last inactive date. 0 - today
   * 1 - yesterday -1 - runtime error 2 - 2 days
   */
  fun getDaysSinceLastInactiveDateString(lastInactiveDate: LocalDate): String {
    return when (val daysSinceLastInactiveDate = getDaysSinceLastInactiveDate(lastInactiveDate)) {
      0 -> "today"
      1 -> "yesterday"
      -1 -> throw IllegalArgumentException("lastInactiveDate is in the future")
      else -> "$daysSinceLastInactiveDate days ago"
    }
  }

  fun getVotingMembersGroupName() =
      if (IS_VOTER_REGISTRATION_TEST_MODE_ENABLED) VOTING_MEMBERS_GROUP_TEST
      else VOTING_MEMBERS_GROUP

  companion object {
    const val IS_VOTER_REGISTRATION_TEST_MODE_ENABLED = false
    const val MEMBER_IN_GOOD_STANDING_DAYS = 90
    const val VOTING_MEMBERS_GROUP_TEST = "Voting Members - Test"
    const val VOTING_MEMBERS_GROUP = "Voting Members"
  }
}
