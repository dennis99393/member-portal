package org.dallasmakerspace.server.voterregistration

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.dallasmakerspace.server.common.Time
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class VoterRegistrationManagerTest {

  private val time: Time = mock()
  private val voterRegistrationManager = VoterRegistrationManager(time)

  @Before
  fun setUp() {
    whenever(time.getToday()).thenReturn(LocalDate(2025, 1, 1).toJavaLocalDate())
  }

  @Test
  fun getDaysSinceLastInactiveDateString() {
    getDaysSinceLastInactiveDateString_verify(LocalDate(2024, 12, 1), "31 days ago")
    getDaysSinceLastInactiveDateString_verify(LocalDate(2024, 12, 31), "yesterday")
    getDaysSinceLastInactiveDateString_verify(LocalDate(2025, 1, 1), "today")
  }

  private fun getDaysSinceLastInactiveDateString_verify(localDate: LocalDate, s: String) {
    val daysSinceLastInactiveDateString =
        voterRegistrationManager.getDaysSinceLastInactiveDateString(localDate)
    assertEquals(s, daysSinceLastInactiveDateString)
  }

  @Test
  fun getDaysSinceLastInactiveDate() {
    getDaysSinceLastInactiveDate_verify(LocalDate(2024, 12, 1), 31)
    getDaysSinceLastInactiveDate_verify(LocalDate(2024, 12, 31), 1)
    getDaysSinceLastInactiveDate_verify(LocalDate(2025, 1, 1), 0)
    getDaysSinceLastInactiveDate_verify(LocalDate(2025, 1, 2), -1)
  }

  private fun getDaysSinceLastInactiveDate_verify(lastInactiveDate: LocalDate, expectedDays: Any) {
    val daysSinceLastInactiveDate =
        voterRegistrationManager.getDaysSinceLastInactiveDate(lastInactiveDate)
    assertEquals(expectedDays, daysSinceLastInactiveDate)
  }
}
