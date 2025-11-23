package org.dallasmakerspace.doorcontroller

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.core.LoggerFactory

/**
 * HTTP client for communicating with RFID door controllers via their web interface.
 *
 * @param ip IP address of the controller
 * @param username Web interface username
 * @param password Web interface password
 * @param loggerFactory Logger factory for creating logger instance
 */
class HttpRfidClient(
    private val ip: String,
    private val username: String,
    private val password: String,
    loggerFactory: LoggerFactory,
) : Closeable {
  private val log = loggerFactory.create(HttpRfidClient::class.java)
  private val httpClient = HttpClient(CIO)
  private val baseUrl = "http://$ip"

  /**
   * Logs into the door controller web interface.
   *
   * @return true if login successful
   */
  private suspend fun login(): Boolean =
      withContext(Dispatchers.IO) {
        try {
          log.debug("Logging into door controller at $ip")

          val response: HttpResponse =
              httpClient.submitForm(
                  url = "$baseUrl/ACT_ID_1",
                  formParameters =
                      Parameters.build {
                        append("username", username)
                        append("pwd", password)
                        append("logId", "20101222")
                      },
              )

          val success = response.status.isSuccess()
          if (success) {
            log.debug("Successfully logged into $ip")
          } else {
            log.error("Failed to login to $ip: ${response.status}")
          }
          success
        } catch (e: Exception) {
          log.error("Login failed for $ip: ${e.message}", e)
          false
        }
      }

  /**
   * Reads recent badge swipe events from the controller web interface.
   *
   * @return List of badge swipe events
   */
  suspend fun readRecentSwipes(): List<BadgeSwipeEvent> =
      withContext(Dispatchers.IO) {
        try {
          // Login first
          if (!login()) {
            log.error("Cannot read swipes from $ip - login failed")
            return@withContext emptyList()
          }

          log.debug("Requesting swipes page from $ip")

          // Request swipes page
          val response: HttpResponse =
              httpClient.submitForm(
                  url = "$baseUrl/ACT_ID_21",
                  formParameters = Parameters.build { append("s4", "Swipe") },
              )

          if (!response.status.isSuccess()) {
            log.error("Failed to fetch swipes from $ip: ${response.status}")
            return@withContext emptyList()
          }

          val html = response.bodyAsText()
          log.debug("Received HTML response (${html.length} chars)")

          parseSwipeEvents(html)
        } catch (e: Exception) {
          log.error("Failed to read swipes from $ip: ${e.message}", e)
          throw Exception("Failed to read swipes from $ip: ${e.message}", e)
        }
      }

  /**
   * Parses swipe events from HTML table.
   *
   * Expected format:
   * <tr class=Y><td>517106</td><td>21545540</td><td>&nbsp;</td><td>Allow IN[#3DOOR]</td>
   * <td>2025-11-22 12:08:38</td></tr>
   */
  private fun parseSwipeEvents(html: String): List<BadgeSwipeEvent> {
    val events = mutableListOf<BadgeSwipeEvent>()

    log.debug("Parsing swipe events from HTML")

    // Extract server's current time to calculate clock delta
    // Format: "Page  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 2025-11-22 22:28:00</p>"
    val serverTimePattern =
        """Page\s+&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\s+(.*?)<""".toRegex()

    val serverTimeMatch = serverTimePattern.find(html)
    val timeDeltaMs =
        if (serverTimeMatch != null) {
          val serverTimeStr = serverTimeMatch.groupValues[1].trim()
          log.info("Found server time in pagination: '$serverTimeStr'")

          val serverTime = parseDateTime(serverTimeStr)
          val localTime = System.currentTimeMillis()
          val delta = localTime - serverTime
          val deltaMinutes = delta / 1000 / 60
          val deltaHours = deltaMinutes / 60

          // Format timestamps for readability
          val serverTimeFormatted =
              java.time.Instant.ofEpochMilli(serverTime)
                  .atZone(java.time.ZoneId.of("America/Chicago"))
                  .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          val localTimeFormatted =
              java.time.Instant.ofEpochMilli(localTime)
                  .atZone(java.time.ZoneId.of("America/Chicago"))
                  .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

          log.info("Time correction calculation:")
          log.info("  Server time: $serverTimeStr → $serverTime ms → $serverTimeFormatted CST")
          log.info("  Local time:  $localTime ms → $localTimeFormatted CST")
          log.info("  Delta: ${delta}ms = ${deltaMinutes} minutes = $deltaHours hours")
          delta
        } else {
          log.error("Could not find server time in pagination! Timestamps will be inaccurate!")
          log.error(
              "Looking for pattern: 'Page  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; YYYY-MM-DD HH:MM:SS</p>'"
          )
          log.error("HTML length: ${html.length} chars")
          // Show a larger snippet to help debug
          val snippet = html.substring(0, minOf(1500, html.length))
          log.error("HTML snippet (first 1500 chars): $snippet")
          0L
        }

    // Regex to extract table rows (both Y=allowed and N=denied)
    val rowPattern = """<tr\s+class=[YN]>(.*?)</tr>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val cellPattern = """<td>(.*?)</td>""".toRegex(RegexOption.DOT_MATCHES_ALL)

    val rows = rowPattern.findAll(html)

    for (row in rows) {
      try {
        val cells = cellPattern.findAll(row.value).map { it.groupValues[1].trim() }.toList()

        if (cells.size >= 5) {
          // Parse cells
          val recordId = cells[0]
          val cardNo = cells[1]
          val status = cells[3]
          val dateTime = cells[4]

          // Extract door number from status like "Allow IN[#3DOOR]"
          val doorMatch = """#(\d)DOOR""".toRegex().find(status)
          val doorNumber = doorMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

          // Parse badge number (in comma format) and convert to 10-digit
          val badgeCommaFormat = cardNo.toIntOrNull() ?: 0
          val badge =
              try {
                RfidUtils.commaFormatToTenDigit(badgeCommaFormat)
              } catch (e: Exception) {
                log.debug("Badge conversion failed for $badgeCommaFormat, using as-is")
                badgeCommaFormat
              }

          // Parse timestamp and adjust for server clock delta
          val rawTimestamp = parseDateTime(dateTime)
          val adjustedTimestamp = rawTimestamp + timeDeltaMs

          // Determine event type
          val eventType =
              when {
                status.contains("Allow", ignoreCase = true) -> SwipeEventType.ACCESS_GRANTED
                status.contains("Forbid", ignoreCase = true) -> SwipeEventType.ACCESS_DENIED
                else -> SwipeEventType.UNKNOWN
              }

          events.add(
              BadgeSwipeEvent(
                  controllerName = ip,
                  doorNumber = doorNumber,
                  badge = badge,
                  timestamp = adjustedTimestamp,
                  eventType = eventType,
                  friendlyDoorName = "door_$doorNumber",
              )
          )

          log.debug(
              "Parsed event: recordId=$recordId, badge=$badge, door=$doorNumber, " +
                  "status=$status, rawTime=$dateTime ($rawTimestamp), " +
                  "delta=${timeDeltaMs}ms, adjusted=$adjustedTimestamp"
          )
        }
      } catch (e: Exception) {
        log.warn("Failed to parse swipe event row: ${e.message}")
      }
    }

    log.info("Parsed ${events.size} swipe events from $ip")
    return events
  }

  /** Parses datetime string like "2025-11-22 12:08:38" to Unix timestamp in milliseconds. */
  private fun parseDateTime(dateTime: String): Long {
    return try {
      log.debug("Parsing datetime: '$dateTime'")
      val parts = dateTime.split(" ")
      log.debug("  Split into ${parts.size} parts: ${parts.joinToString(", ")}")

      if (parts.size == 2) {
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")

        log.debug("  Date parts: ${dateParts.joinToString("-")}")
        log.debug("  Time parts: ${timeParts.joinToString(":")}")

        if (dateParts.size == 3 && timeParts.size == 3) {
          val year = dateParts[0].toInt()
          val month = dateParts[1].toInt()
          val day = dateParts[2].toInt()
          val hour = timeParts[0].toInt()
          val minute = timeParts[1].toInt()
          val second = timeParts[2].toInt()

          log.debug(
              "  Parsed: year=$year, month=$month, day=$day, " +
                  "hour=$hour, minute=$minute, second=$second"
          )

          // Create timestamp using java.time
          val result =
              java.time.LocalDateTime.of(year, month, day, hour, minute, second)
                  .atZone(java.time.ZoneId.of("America/Chicago"))
                  .toInstant()
                  .toEpochMilli()

          log.debug("  Result: $result ms")
          result
        } else {
          log.warn("  Invalid part counts: date=${dateParts.size}, time=${timeParts.size}")
          0L
        }
      } else {
        log.warn("  Invalid split count: ${parts.size} (expected 2)")
        0L
      }
    } catch (e: Exception) {
      log.error("Failed to parse datetime: '$dateTime' - ${e.message}", e)
      0L
    }
  }

  override fun close() {
    httpClient.close()
    log.debug("Closed HTTP client for $ip")
  }
}
