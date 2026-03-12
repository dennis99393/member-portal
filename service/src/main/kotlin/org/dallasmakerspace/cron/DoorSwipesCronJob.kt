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
    private const val DEBUG_EVENT_SAMPLE_SIZE = 10
  }

  @Suppress("MagicNumber")
  override suspend fun run(params: DoorSwipesCronJobParams): String {
    val startTime = System.currentTimeMillis()

    val result = doorControllerService.readRecentSwipes()

    var insertedCount = 0
    if (result.events.isNotEmpty()) {
      insertedCount = doorControllerService.persistEvents(result.events)
    }

    val controllersQueried =
        result.errors.size + result.events.map { it.controllerName }.distinct().size
    val timeTaken = System.currentTimeMillis() - startTime
    log.info(
        "DoorSwipesCronJob complete in ${timeTaken}ms: " +
            "controllers=$controllersQueried " +
            "retrieved=${result.events.size} " +
            "inserted=$insertedCount " +
            "duplicates=${result.events.size - insertedCount} " +
            "errors=${result.errors.size}")

    if (result.errors.isNotEmpty()) {
      result.errors.forEach { error -> log.error("Controller error: $error") }
    }

    if (log.isDebugEnabled) {
      val grantedCount = result.events.count { it.eventType == SwipeEventType.ACCESS_GRANTED }
      val deniedCount = result.events.count { it.eventType == SwipeEventType.ACCESS_DENIED }
      val unknownCount = result.events.count { it.eventType == SwipeEventType.UNKNOWN }
      log.debug("Event breakdown: granted=$grantedCount denied=$deniedCount unknown=$unknownCount")
      result.events.take(DEBUG_EVENT_SAMPLE_SIZE).forEach { event ->
        val timestamp = DATE_FORMATTER.format(Instant.ofEpochMilli(event.timestamp))
        val eventTypeStr =
            when (event.eventType) {
              SwipeEventType.ACCESS_GRANTED -> "GRANTED"
              SwipeEventType.ACCESS_DENIED -> "DENIED"
              SwipeEventType.UNKNOWN -> "UNKNOWN"
            }
        log.debug(
            "[$timestamp] Badge ${event.badge.toString().padStart(10, '0')} - $eventTypeStr" +
                " - ${event.friendlyDoorName} (${event.controllerName} door ${event.doorNumber})")
      }
    }

    val logBuffer = (log as? AppInMemoryLogger)?.getLog() ?: ""
    (log as? AppInMemoryLogger)?.clear()
    return logBuffer
  }
}
