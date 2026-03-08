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
    // Use MariaDB server-side statement timeout (1.5s) so the DB kills the query and frees the
    // JDBC connection. withTimeoutOrNull does NOT work here because blocking JDBC calls on
    // Dispatchers.IO are not responsive to coroutine cancellation, which exhausts the connection
    // pool under load.
    val query =
        """
      SET STATEMENT max_statement_time=1.5 FOR
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

    val result =
        try {
          genericRepository.getReportData(query)
        } catch (ex: Exception) {
          log.warn("Timed out or failed fetching events for member: $username - ${ex.message}")
          return emptyList()
        }
    if (result == null) {
      log.warn("No results fetching events for member: $username")
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
          organizerUsername = organizerUsername,
      )
    } ?: emptyList()
  }

  /**
   * Fetches distinct event names that a member has attended, ordered by most recent first.
   *
   * @param username The AD username of the member.
   * @param limit The maximum number of event names to return (default 20).
   * @return A list of event name strings.
   */
  suspend fun getAttendedEventNames(username: String, limit: Int = 20): List<String> {
    val query =
        """
      SET STATEMENT max_statement_time=1.5 FOR
      SELECT e.name
      FROM `dms-calendar`.registrations r
      JOIN `dms-calendar`.events e ON r.event_id = e.id
      WHERE r.ad_username = '$username'
        AND r.attended = 1
      GROUP BY e.name
      ORDER BY MAX(e.event_start) DESC
      LIMIT $limit
    """

    log.debug("Fetching attended event names for member: $username (limit: $limit)")

    val result =
        try {
          genericRepository.getReportData(query)
        } catch (ex: Exception) {
          log.warn("Failed fetching attended event names for member: $username - ${ex.message}")
          return emptyList()
        }

    return result?.data?.mapNotNull { row -> row["name"]?.toString()?.takeIf { it.isNotBlank() } }
        ?: emptyList()
  }

  /**
   * Fetches the distinct organizer usernames for events a member has attended, ranked by how many
   * of their events the member attended (most attended organizer first).
   *
   * @param username The AD username of the member.
   * @param limit The maximum number of organizer usernames to return (default 5).
   * @return A list of organizer AD usernames, highest-frequency first.
   */
  suspend fun getAttendedOrganizers(username: String, limit: Int = 5): List<String> {
    val query =
        """
      SET STATEMENT max_statement_time=1.5 FOR
      SELECT c.ad_username
      FROM `dms-calendar`.registrations r
      JOIN `dms-calendar`.events e ON r.event_id = e.id
      LEFT JOIN `dms-calendar`.contacts c ON e.contact_id = c.id
      WHERE r.ad_username = '$username'
        AND r.attended = 1
        AND c.ad_username IS NOT NULL
      GROUP BY c.ad_username
      ORDER BY COUNT(*) DESC
      LIMIT $limit
    """

    log.debug("Fetching top attended organizers for member: $username (limit: $limit)")

    val result =
        try {
          genericRepository.getReportData(query)
        } catch (ex: Exception) {
          log.warn("Failed fetching attended organizers for member: $username - ${ex.message}")
          return emptyList()
        }

    return result?.data?.mapNotNull { row ->
      row["ad_username"]?.toString()?.takeIf { it.isNotBlank() }
    } ?: emptyList()
  }

  /**
   * Fetches upcoming events organized by any of the given organizer usernames.
   *
   * @param organizers The list of organizer AD usernames to fetch events for.
   * @param limit The maximum number of events to return (default 4).
   * @return A list of [EventSummary] objects for upcoming events.
   */
  suspend fun getUpcomingEventsByOrganizers(
      organizers: List<String>,
      limit: Int = 4,
  ): List<EventSummary> {
    if (organizers.isEmpty()) {
      return emptyList()
    }

    val organizerList = organizers.joinToString(",") { "'${it.replace("'", "''")}'" }

    val query =
        """
      SET STATEMENT max_statement_time=1.5 FOR
      SELECT e.id, e.name,
             CONVERT_TZ(e.event_start, '+00:00', 'America/Chicago') AS event_start_cst,
             e.status,
             c.ad_username AS organizer_username
      FROM `dms-calendar`.events e
      JOIN `dms-calendar`.contacts c ON e.contact_id = c.id
      WHERE e.event_start >= NOW()
        AND c.ad_username IN ($organizerList)
      ORDER BY e.event_start ASC
      LIMIT $limit
    """

    log.debug("Fetching upcoming events for ${organizers.size} organizers (limit: $limit)")

    val result =
        try {
          genericRepository.getReportData(query)
        } catch (ex: Exception) {
          log.warn("Failed fetching upcoming events by organizers - ${ex.message}")
          return emptyList()
        }

    return result?.data?.map { row ->
      val id = (row["id"] as? Number)?.toInt() ?: 0
      val name = row["name"]?.toString() ?: ""
      val eventStart = row["event_start_cst"]?.toString() ?: ""
      val status = row["status"]?.toString() ?: ""
      val organizerUsername = row["ad_username"]?.toString()

      EventSummary(
          id = id,
          name = name,
          eventStart = eventStart,
          status = status,
          organizerUsername = organizerUsername,
      )
    } ?: emptyList()
  }

  /**
   * Fetches upcoming events whose names match any of the given keywords.
   *
   * @param keywords The list of keywords to search for in event names.
   * @param limit The maximum number of events to return (default 5).
   * @return A list of [EventSummary] objects for upcoming matching events.
   */
  suspend fun getUpcomingEventsByKeywords(
      keywords: List<String>,
      limit: Int = 5,
  ): List<EventSummary> {
    if (keywords.isEmpty()) {
      return emptyList()
    }

    val orClause =
        keywords.joinToString(" OR ") { kw ->
          val escaped =
              kw.replace("'", "''").replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
          "LOWER(e.name) LIKE '%$escaped%' ESCAPE '\\\\'"
        }

    val query =
        """
      SET STATEMENT max_statement_time=1.5 FOR
      SELECT e.id, e.name,
             CONVERT_TZ(e.event_start, '+00:00', 'America/Chicago') AS event_start_cst,
             e.status,
             c.ad_username AS organizer_username
      FROM `dms-calendar`.events e
      LEFT JOIN `dms-calendar`.contacts c ON e.contact_id = c.id
      WHERE e.event_start >= NOW()
        AND ($orClause)
      ORDER BY e.event_start ASC
      LIMIT $limit
    """

    log.debug("Fetching upcoming events by ${keywords.size} keywords (limit: $limit)")

    val result =
        try {
          genericRepository.getReportData(query)
        } catch (ex: Exception) {
          log.warn("Failed fetching upcoming events by keywords - ${ex.message}")
          return emptyList()
        }

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
          organizerUsername = organizerUsername,
      )
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
    val organizerUsername: String? = null,
)
