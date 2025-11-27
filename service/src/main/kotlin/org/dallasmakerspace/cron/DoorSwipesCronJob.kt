package org.dallasmakerspace.cron

import dagger.Reusable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.core.logging.AppInMemoryLogger
import org.dallasmakerspace.doorcontroller.DoorControllerService
import org.dallasmakerspace.doorcontroller.SwipeEventType

/**
 * Cron job for reading recent badge swipe events from all door controllers. This is primarily used
 * for testing the door controller read operations.
 */
@Reusable
class DoorSwipesCronJob
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val doorControllerService: DoorControllerService,
) : CronJob<DoorSwipesCronJobParams>(DoorSwipesCronJobParams::class, loggerFactory) {

  companion object {
    private val DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/Chicago"))
  }

  override suspend fun run(params: DoorSwipesCronJobParams): String {
    log.info(
        "**************************************************************************************"
    )
    log.info("Running Door Swipes Cron Job; params: $params")
    log.info(
        "**************************************************************************************"
    )
    val startTime = System.currentTimeMillis()

    val result = doorControllerService.readRecentSwipes(params.minutes)

    log.info("Retrieved ${result.events.size} swipe events from all controllers")

    // Persist events to database
    var insertedCount = 0
    if (result.events.isNotEmpty()) {
      log.info("Persisting ${result.events.size} events to database...")
      insertedCount = doorControllerService.persistEvents(result.events)
      log.info("Successfully inserted $insertedCount events (${result.events.size - insertedCount} duplicates skipped)")
    }

    // Print summary
    log.info("")
    log.info("=== SUMMARY ===")
    log.info("Events retrieved:    ${result.events.size}")
    log.info("Events persisted:    $insertedCount")
    log.info("Duplicates skipped:  ${result.events.size - insertedCount}")
    log.info("Controllers queried: ${result.errors.size + (result.events.map { it.controllerName }.distinct().size)}")
    log.info("Errors encountered:  ${result.errors.size}")

    // Event breakdown by type
    val grantedCount = result.events.count { it.eventType == SwipeEventType.ACCESS_GRANTED }
    val deniedCount = result.events.count { it.eventType == SwipeEventType.ACCESS_DENIED }
    val unknownCount = result.events.count { it.eventType == SwipeEventType.UNKNOWN }
    log.info("")
    log.info("Event breakdown:")
    log.info("  Access granted: $grantedCount")
    log.info("  Access denied:  $deniedCount")
    log.info("  Unknown:        $unknownCount")

    if (result.errors.isNotEmpty()) {
      log.error("")
      log.error("Encountered ${result.errors.size} error(s) while reading swipes:")
      result.errors.forEach { error -> log.error("  ERROR: $error") }
      log.error("")
      log.error("This may indicate:")
      log.error("  1. Network connectivity issues with door controllers")
      log.error("  2. Incorrect protocol command codes (0x2340 may not be correct)")
      log.error("  3. Controllers are offline or unreachable")
      log.error("  4. Protocol timeout or response format mismatch")
      log.error("Check the DEBUG logs above for detailed packet information")
    }

    if (result.events.isEmpty()) {
      log.info("")
      log.info("No swipe events found in the past ${params.minutes} minutes")
    } else {
      log.info("")
      log.info("Recent swipe events (most recent first):")
      result.events.take(10).forEach { event ->
        val timestamp = DATE_FORMATTER.format(Instant.ofEpochMilli(event.timestamp))
        val eventTypeStr =
            when (event.eventType) {
              SwipeEventType.ACCESS_GRANTED -> "GRANTED"
              SwipeEventType.ACCESS_DENIED -> "DENIED "
              SwipeEventType.UNKNOWN -> "UNKNOWN"
            }
        log.info(
            "  [$timestamp] Badge ${event.badge.toString().padStart(10, '0')} - $eventTypeStr" +
                " - ${event.friendlyDoorName} " +
                "(${event.controllerName} door ${event.doorNumber})"
        )
      }
      if (result.events.size > 10) {
        log.info("  ... and ${result.events.size - 10} more events")
      }
    }

    val timeTaken = System.currentTimeMillis() - startTime
    log.info("Finished running DoorSwipesCronJob in $timeTaken ms")

    val logBuffer = (log as? AppInMemoryLogger)?.getLog() ?: ""
    (log as? AppInMemoryLogger)?.clear()
    return logBuffer
  }
}
