package org.dallasmakerspace.cron

import dagger.Reusable
import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.smartwaiver.ISmartwaiverApiClient
import org.dallasmakerspace.smartwaiver.SmartWaiverRepository

private const val EVENT_NEW_WAIVER = "new-waiver"

@Reusable
class SmartWaiverProcessQueueJob
@Inject
constructor(
    private val smartwaiverApiClient: ISmartwaiverApiClient,
    private val smartWaiverRepository: SmartWaiverRepository,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  suspend fun run(): String {
    val entries = smartWaiverRepository.getUnprocessedQueueEntries()
    log.info("Found ${entries.size} unprocessed queue entries")

    var processed = 0
    var failed = 0

    for ((uniqueId, event) in entries) {
      if (event != EVENT_NEW_WAIVER) {
        log.warn("Skipping unexpected event type '$event' for waiver $uniqueId")
        continue
      }
      try {
        val t0 = System.currentTimeMillis()
        val waiver = smartwaiverApiClient.getWaiver(uniqueId)
        log.info("Fetched waiver $uniqueId (${System.currentTimeMillis() - t0}ms)")

        smartWaiverRepository.insertWaiverAndMarkProcessed(waiver, uniqueId)
        log.info("Processed waiver $uniqueId")
        processed++
      } catch (e: Exception) {
        log.error("Failed to process waiver $uniqueId: ${e.message}")
        failed++
      }
    }

    return "total=${entries.size}\nprocessed=$processed\nfailed=$failed"
  }
}
