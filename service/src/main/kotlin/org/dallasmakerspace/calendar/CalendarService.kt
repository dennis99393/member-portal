package org.dallasmakerspace.calendar

import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory

/**
 * Service for calendar events operations.
 */
class CalendarService @Inject constructor(
    private val calendarRepository: CalendarRepository,
    loggerFactory: LoggerFactory
) {
  private val log = loggerFactory.create(javaClass)

  /**
   * Gets the most recent events organized by a member.
   *
   * @param username The username of the member.
   * @param limit The maximum number of events to return (default 5).
   * @return A list of [EventSummary] objects.
   */
  suspend fun getEventsOrganizedByMember(username: String, limit: Int = 5): List<EventSummary> {
    log.info("Getting events organized by member: $username")
    return calendarRepository.getEventsOrganizedByMember(username, limit)
  }
}
