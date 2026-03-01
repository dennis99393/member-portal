package org.dallasmakerspace.calendar

import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.db.master.GenericRepository

/** Repository for calendar events data. */
class CalendarRepository
@Inject
constructor(private val genericRepository: GenericRepository, loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)

  /**
   * Fetches events organized by a member, prioritized by proximity to today. Events closest to the
   * current date (whether past or future) are returned first.
   *
   * @param username The username of the member to fetch events for.
   * @param limit The maximum number of events to return (default 5).
   * @return A list of [EventSummary] objects.
   */
  suspend fun getEventsOrganizedByMember(username: String, limit: Int = 5): List<EventSummary> {
    val query =
        """
      SELECT * FROM (
        SELECT
            e.id,
            e.name,
            CONVERT_TZ(e.event_start, '+00:00', 'America/Chicago') AS event_start_cst,
            e.status
        FROM
            `dms-calendar`.events e
        JOIN
            `dms-calendar`.contacts c ON e.contact_id = c.id
        WHERE
            c.ad_username = '$username'
        ORDER BY
            ABS(TIMESTAMPDIFF(SECOND, e.event_start, NOW())) ASC
        LIMIT $limit
      ) AS closest_events
      ORDER BY event_start_cst DESC
    """

    log.debug("Fetching events organized by member: $username (limit: $limit)")

    val result = withTimeoutOrNull(1_500) { genericRepository.getReportData(query) }
    if (result == null) {
      log.warn("Timed out fetching events for member: $username")
      return emptyList()
    }

    return result.data.map { row ->
      val id = (row["id"] as? Number)?.toInt() ?: 0
      val name = row["name"]?.toString() ?: ""
      val eventStart = row["event_start_cst"]?.toString() ?: ""
      val status = row["status"]?.toString() ?: ""

      EventSummary(id = id, name = name, eventStart = eventStart, status = status)
    }
  }

  /**
   * Checks if there are any prerequisite events for a given AD group within the last 90 days.
   *
   * @param groupName The AD group name to check for prerequisite events.
   * @return true if any prerequisite events exist, false otherwise.
   */
  suspend fun hasPrerequisiteEvents(groupName: String): Boolean {
    val query =
        """
      SELECT EXISTS(
        SELECT 1
        FROM `dms-calendar`.events e
        JOIN `dms-calendar`.prerequisites p ON e.fulfills_prerequisite_id = p.id
        WHERE
            e.event_start >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
            AND e.event_start < DATE_ADD(CURDATE(), INTERVAL 30 DAY)
            AND p.ad_group = '${groupName.replace("'", "''")}'
      ) AS has_events
    """

    log.debug("Checking prerequisite events for group: $groupName")

    val result = genericRepository.getReportData(query)
    val hasEvents = result?.data?.firstOrNull()?.get("has_events")
    return hasEvents == 1L || hasEvents == 1 || hasEvents == true
  }

  /**
   * Fetches upcoming prerequisite events for multiple AD groups.
   *
   * @param groupNames The list of AD group names to fetch events for.
   * @return A list of [EventSummary] objects for upcoming events.
   */
  suspend fun getUpcomingPrerequisiteEvents(groupNames: List<String>): List<EventSummary> {
    if (groupNames.isEmpty()) {
      return emptyList()
    }

    // Build SQL IN clause with escaped group names
    val groupNamesList = groupNames.joinToString(",") { "'${it.replace("'", "''")}'" }

    val query =
        """
      SELECT
          e.id,
          e.name,
          CONVERT_TZ(e.event_start, '+00:00', 'America/Chicago') AS event_start_cst,
          e.status,
          c.ad_username as organizer_username
      FROM `dms-calendar`.events e
      JOIN `dms-calendar`.prerequisites p ON e.fulfills_prerequisite_id = p.id
      LEFT JOIN `dms-calendar`.contacts c ON e.contact_id = c.id
      WHERE
          e.event_start >= NOW()
          AND p.ad_group IN ($groupNamesList)
      ORDER BY e.event_start ASC
      LIMIT 20
    """

    log.debug("Fetching upcoming prerequisite events for ${groupNames.size} groups")

    val result = genericRepository.getReportData(query)

    return result?.data?.map { row ->
      val id = (row["id"] as? Number)?.toInt() ?: 0
      val name = row["name"]?.toString() ?: ""
      val eventStart = row["event_start_cst"]?.toString() ?: ""
      val status = row["status"]?.toString() ?: ""
      val organizerUsername = row["organizer_username"]?.toString()

      EventSummary(
          id = id,
          name = name,
          eventStart = eventStart,
          status = status,
          organizerUsername = organizerUsername)
    } ?: emptyList()
  }
}

/** Represents a summary of a calendar event. */
@Serializable
data class EventSummary(
    val id: Int,
    val name: String,
    val eventStart: String,
    val status: String,
    val organizerUsername: String? = null
)
