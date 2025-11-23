package org.dallasmakerspace.doorcontroller

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.core.LoggerFactory
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
    loggerFactory: LoggerFactory
) {
    private val log = loggerFactory.create(DoorControllerService::class.java)
    private val clients: Map<String, HttpRfidClient> = config.controllers.associate { controller ->
        controller.name to HttpRfidClient(
            ip = controller.ip,
            username = config.username,
            password = config.password,
            loggerFactory = loggerFactory
        )
    }

    /**
     * Reads recent badge swipe events from all controllers.
     *
     * @param minutes Number of minutes to look back
     * @return BadgeSwipesResult containing all events and any errors
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun readRecentSwipes(minutes: Int): BadgeSwipesResult {
        log.info("Reading badge swipes from past $minutes minutes across all controllers")

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
     * Closes all client connections.
     */
    fun close() {
        clients.values.forEach { it.close() }
        log.info("Closed all door controller connections")
    }
}
