package org.dallasmakerspace.calendar

import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory

/** Service for calendar events operations. */
class CalendarService
@Inject
constructor(private val calendarRepository: CalendarRepository, loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)

  /**
   * Gets events organized by a member, prioritized by proximity to today. Events closest to the
   * current date (whether past or future) are returned first.
   *
   * @param username The username of the member.
   * @param limit The maximum number of events to return (default 5).
   * @return A list of [EventSummary] objects.
   */
  suspend fun getEventsOrganizedByMember(username: String, limit: Int = 5): List<EventSummary> {
    log.info("Getting events organized by member: $username")
    return calendarRepository.getEventsOrganizedByMember(username, limit)
  }

  /**
   * Checks if there are any prerequisite events for a given AD group.
   *
   * @param groupName The AD group name to check.
   * @return true if prerequisite events exist within the time range, false otherwise.
   */
  suspend fun hasPrerequisiteEvents(groupName: String): Boolean {
    log.info("Checking prerequisite events for group: $groupName")
    return calendarRepository.hasPrerequisiteEvents(groupName)
  }

  /**
   * Gets upcoming prerequisite events for multiple AD groups.
   *
   * @param groupNames The list of AD group names.
   * @return A list of [EventSummary] objects for upcoming events.
   */
  suspend fun getUpcomingPrerequisiteEvents(groupNames: List<String>): List<EventSummary> {
    log.info("Getting upcoming prerequisite events for ${groupNames.size} groups")
    return calendarRepository.getUpcomingPrerequisiteEvents(groupNames)
  }
}
