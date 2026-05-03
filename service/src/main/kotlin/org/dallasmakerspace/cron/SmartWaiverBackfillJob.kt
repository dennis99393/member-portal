package org.dallasmakerspace.cron

import dagger.Reusable
import java.time.LocalDate
import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.smartwaiver.ISmartwaiverApiClient
import org.dallasmakerspace.smartwaiver.SmartWaiverRepository

private val BACKFILL_START = LocalDate.of(2024, 8, 17)
private const val WINDOW_DAYS = 30L

data class BackfillResult(
    val from: LocalDate,
    val to: LocalDate,
    val fetched: Int,
    val inserted: Int,
    val skipped: Int,
    val done: Boolean,
) {
  fun toText(): String =
      "from=$from\nto=$to\nfetched=$fetched\ninserted=$inserted\nskipped=$skipped\ndone=$done"
}

@Reusable
class SmartWaiverBackfillJob
@Inject
constructor(
    private val smartwaiverApiClient: ISmartwaiverApiClient,
    private val smartWaiverRepository: SmartWaiverRepository,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun run(): BackfillResult {
    val today = LocalDate.now()

    val t0 = System.currentTimeMillis()
    val maxDate = smartWaiverRepository.getMaxDateCompleted()
    log.info("getMaxDateCompleted=${maxDate} (${System.currentTimeMillis() - t0}ms)")

    val from = maxDate?.plusDays(1) ?: BACKFILL_START

    if (!from.isBefore(today)) {
      log.info("Backfill complete — from=$from is not before today=$today")
      return BackfillResult(from, from.plusDays(WINDOW_DAYS - 1), 0, 0, 0, done = true)
    }

    val to = minOf(from.plusDays(WINDOW_DAYS - 1), today)
    log.info("Processing window $from – $to")

    val t1 = System.currentTimeMillis()
    val summaries = smartwaiverApiClient.getWaiverDetails(from, to)
    log.info(
        "getWaiverDetails: ${summaries.size} summaries fetched (${System.currentTimeMillis() - t1}ms)")

    val t2 = System.currentTimeMillis()
    val existingIds = smartWaiverRepository.findExistingWaiverIds(summaries.map { it.waiverId })
    log.info(
        "findExistingWaiverIds: ${existingIds.size} already in DB (${System.currentTimeMillis() - t2}ms)")

    val toInsert = summaries.filter { it.waiverId !in existingIds }
    log.info("${toInsert.size} new waivers to insert")

    val t3 = System.currentTimeMillis()
    smartWaiverRepository.insertSmartWaiverBatchFromSummaries(toInsert)
    log.info("insertSmartWaiverBatchFromSummaries: done (${System.currentTimeMillis() - t3}ms)")

    return BackfillResult(
        from, to, summaries.size, toInsert.size, existingIds.size, done = !to.isBefore(today))
  }
}
