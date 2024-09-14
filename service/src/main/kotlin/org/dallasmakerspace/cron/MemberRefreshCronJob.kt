package org.dallasmakerspace.cron

import dagger.Reusable
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.core.logging.AppInMemoryLogger
import org.dallasmakerspace.members.MemberComparator
import org.dallasmakerspace.members.MemberService

/**
 * This class is responsible for refreshing the member list from Active Directory. It is intended to
 * be run on a regular schedule. It will query Active Directory for the list of members in the DB
 * and compare the two lists. It will then notify observers of any changes.
 */
@Reusable
class MemberRefreshCronJob
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberService: MemberService,
    private val memberComparator: MemberComparator,
    private val activeDirectoryService: ActiveDirectoryService
) : CronJob<MemberRefreshCronJobParams>(MemberRefreshCronJobParams::class, loggerFactory) {

  @Suppress("TooGenericExceptionCaught")
  override suspend fun run(params: MemberRefreshCronJobParams): String {
    log.info(
        "**************************************************************************************")
    log.info("Running Cron Job to refresh DB users; params: $params")
    log.info(
        "**************************************************************************************")
    val startTime = System.currentTimeMillis()

    val dbMembers = memberService.getAllMembers()
    val adMembers = activeDirectoryService.getMembersByUsernameList(dbMembers.map { it.username })

    log.info("Found members in DB: ${dbMembers.size}; Found users in AD: ${adMembers.size}")

    // Build list of Pair<> of dbMembers to adMembers
    val memberPairs =
        dbMembers
            .map { dbMember ->
              val adMember = adMembers[dbMember.username]
              Pair(dbMember, adMember)
            }
            .toMutableList()

    var membersDisabled = 0
    var membersEnabled = 0
    while (memberPairs.isNotEmpty()) {
      val batchSize = minOf(MAX_MEMBER_PROCESS_BATCH_SIZE, memberPairs.size)
      val batch = List(batchSize) { memberPairs.removeAt(Random.nextInt(memberPairs.size)) }
      try {
        // Randomly pick a batch to send to memberComparator
        val result = memberComparator.process(batch, log, params.isRunningInShadowMode)
        membersDisabled += result.first
        membersEnabled += result.second
      } catch (e: Exception) {
        log.error("Error processing batch: ${e.message}", e)
      }
      // Sleep for a bit between batches
      withContext(Dispatchers.IO) { Thread.sleep(DELAY_BETWEEN_BATCHES_MILLIS) }
    }

    val timeTaken = System.currentTimeMillis() - startTime
    log.info(
        "Finished running MemberRefreshCronJob in $timeTaken ms; Members processed: ${dbMembers.size};\n" +
            "Members disabled: $membersDisabled; Members enabled: $membersEnabled")
    val logBuffer = (log as? AppInMemoryLogger)?.getLog() ?: ""
    (log as? AppInMemoryLogger)?.clear()
    return logBuffer
  }

  companion object {
    private const val MAX_MEMBER_PROCESS_BATCH_SIZE = 10
    private const val DELAY_BETWEEN_BATCHES_MILLIS = 50L
  }
}
