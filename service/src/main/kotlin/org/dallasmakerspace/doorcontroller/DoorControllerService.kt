package org.dallasmakerspace.doorcontroller

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.db.master.MakerManagerDataRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing door controller access across multiple controllers.
 *
 * This service orchestrates operations across all configured door controllers using HTTP.
 */
@Singleton
class DoorControllerService @Inject constructor(
    private val config: DoorControllerConfig,
    private val doorEventsRepository: DoorEventsRepository,
    private val makerManagerDataRepository: MakerManagerDataRepository,
    loggerFactory: LoggerFactory
) {
    private val log = loggerFactory.create(DoorControllerService::class.java)
    private val clients: Map<String, HttpRfidClient> = config.controllers.associate { controller ->
        controller.name to HttpRfidClient(
            ip = controller.ip,
            username = config.username,
            password = config.password,
            timeoutSeconds = config.timeoutSeconds,
            loggerFactory = loggerFactory
        )
    }

    // Map controller names to their definitions for quick lookup
    private val controllersByName: Map<String, ControllerDefinition> =
        config.controllers.associateBy { it.name }

    /**
     * Reads recent badge swipe events from all controllers.
     *
     * @return BadgeSwipesResult containing all events and any errors
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun readRecentSwipes(): BadgeSwipesResult {
        log.info("Reading badge swipes from all controllers")

        val allEvents = mutableListOf<BadgeSwipeEvent>()
        val errors = mutableListOf<String>()

        // Execute queries in parallel
        coroutineScope {
            config.controllers.map { controller ->
                async {
                    try {
                        val client = checkNotNull(clients[controller.name]) {
                            "Client not found for ${controller.name}"
                        }

                        // HTTP client returns first page of swipes (no time filtering)
                        val events = client.readRecentSwipes()

                        // Update events with proper controller name and friendly door names
                        val updatedEvents = events.map { event ->
                            event.copy(
                                controllerName = controller.name,
                                friendlyDoorName = DoorMapping.getFriendlyName(
                                    controller.name,
                                    event.doorNumber
                                )
                            )
                        }

                        allEvents.addAll(updatedEvents)
                        log.info("Retrieved ${events.size} swipe events from ${controller.name}")
                    } catch (e: Exception) {
                        log.error("Failed to read swipes from ${controller.name}", e)
                        val errorMsg = "${controller.name}: ${e.javaClass.simpleName} - ${e.message}"
                        errors.add(errorMsg)
                    }
                }
            }.awaitAll()
        }

        // Sort events by timestamp (most recent first)
        val sortedEvents = allEvents.sortedByDescending { it.timestamp }

        log.info("Retrieved total of ${sortedEvents.size} swipe events from all controllers")

        return BadgeSwipesResult(
            events = sortedEvents,
            errors = errors
        )
    }

    /**
     * Persists badge swipe events to the database.
     * Converts BadgeSwipeEvents to DoorEvents, looks up user IDs, and saves to AccessControl.events
     *
     * @param events List of badge swipe events to persist
     * @return Number of events successfully inserted
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "LongMethod", "ReturnCount", "MagicNumber")
    suspend fun persistEvents(events: List<BadgeSwipeEvent>): Int {
        if (events.isEmpty()) return 0

        log.info("Persisting ${events.size} badge swipe events to database")

        try {
            // Build map of badge numbers to their swipe dates for batch user ID lookup
            val badgeNumbersWithDates = events.associate { event ->
                val badgeNumber = event.badge.toString().padStart(10, '0')
                val swipeDate = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.timestamp),
                    ZoneId.of("UTC")
                ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                badgeNumber to swipeDate
            }

            // Batch lookup user IDs
            val userIdsByBadge = makerManagerDataRepository.getUserIdsByBadgeNumbers(badgeNumbersWithDates)
            log.debug("Looked up ${userIdsByBadge.size} user IDs for ${events.size} events")

            // Filter out events with null recordId (required for dbindex)
            val validEvents = events.filter { it.recordId != null }
            if (validEvents.size < events.size) {
                log.warn("Skipping ${events.size - validEvents.size} events with null recordId")
            }

            // Convert BadgeSwipeEvents to DoorEvents
            val doorEvents = validEvents.mapNotNull { event ->
                try {
                    // Get controller definition
                    val controller = controllersByName[event.controllerName]
                    if (controller == null) {
                        log.warn("Controller ${event.controllerName} not found in configuration, skipping event")
                        return@mapNotNull null
                    }

                    // Get or create controller ID in database
                    val controllerId = doorEventsRepository.getOrCreateControllerId(
                        serialNumber = controller.serial,
                        name = controller.name,
                        ipAddress = controller.ip
                    )

                    // Look up user ID for this badge
                    val badgeNumber = event.badge.toString().padStart(10, '0')
                    val userId = userIdsByBadge[badgeNumber]

                    // Convert badge number to CN format
                    val cardCN = try {
                        RfidUtils.tenDigitToCommaFormat(event.badge).toString().padStart(10, '0')
                    } catch (e: Exception) {
                        log.debug("Failed to convert badge ${event.badge} to CN format: ${e.message}")
                        event.badge.toString().padStart(10, '0')
                    }

                    // Convert timestamp to LocalDateTime (UTC)
                    val eventDate = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.timestamp),
                        ZoneId.of("UTC")
                    )

                    // Determine granted status from event type
                    // Note: We don't have status/options from HTML page, so we use simplified logic
                    val granted = event.eventType == SwipeEventType.ACCESS_GRANTED

                    // Build note from raw status or event type
                    val note = event.rawStatus ?: event.eventType.name

                    DoorEvent(
                        controllerId = controllerId,
                        slotNumber = event.doorNumber,
                        dbindex = event.recordId!!, // Safe because we filtered for non-null recordId
                        dbindextype = 1, // 1 = from HTTP scraping, 0 = from MDB import
                        accessRequest = 0,
                        granted = granted,
                        status = if (granted) 1 else 128, // Simplified: < 128 = success, >= 128 = failure
                        options = 0, // Not available from HTML page
                        cardNumber = badgeNumber,
                        cardCN = cardCN,
                        userId = userId,
                        date = eventDate,
                        rawData = event.rawStatus ?: "",
                        note = note
                    )
                } catch (e: Exception) {
                    log.error("Failed to convert badge swipe event to door event: ${e.message}", e)
                    null
                }
            }

            // Batch insert events
            val insertedCount = doorEventsRepository.insertEvents(doorEvents)
            log.info("Successfully persisted $insertedCount/${validEvents.size} events to database (${events.size} total retrieved)")
            return insertedCount
        } catch (e: Exception) {
            log.error("Failed to persist events: ${e.message}", e)
            return 0
        }
    }

    /**
     * Closes all client connections.
     */
    fun close() {
        clients.values.forEach { it.close() }
        log.info("Closed all door controller connections")
    }
}
