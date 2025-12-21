package org.dallasmakerspace.calendar

import javax.inject.Inject
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

    val result = genericRepository.getReportData(query)

    return result?.data?.map { row ->
      val id = (row["id"] as? Number)?.toInt() ?: 0
      val name = row["name"]?.toString() ?: ""
      val eventStart = row["event_start_cst"]?.toString() ?: ""
      val status = row["status"]?.toString() ?: ""

      EventSummary(id = id, name = name, eventStart = eventStart, status = status)
    } ?: emptyList()
  }
}

/** Represents a summary of a calendar event. */
@Serializable
data class EventSummary(val id: Int, val name: String, val eventStart: String, val status: String)
