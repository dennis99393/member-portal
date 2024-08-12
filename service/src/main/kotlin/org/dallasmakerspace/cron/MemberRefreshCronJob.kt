package org.dallasmakerspace.cron

import dagger.Reusable
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
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
) : ICronJob {
  val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  override suspend fun run(): String {
    log.info("Running MemberRefreshCronJob")
    val startTime = System.currentTimeMillis()

    val dbMembers = memberService.getAllMembers()
    val adMembers = activeDirectoryService.getMembersByUsernameList(dbMembers.map { it.username })

    log.info("DB Members: ${dbMembers.size}; AD Members: ${adMembers.size}")

    // Build list of Pair<> of dbMembers to adMembers
    val memberPairs =
        dbMembers
            .map { dbMember ->
              val adMember = adMembers[dbMember.username]
              Pair(dbMember, adMember)
            }
            .toMutableList()

    while (memberPairs.isNotEmpty()) {
      val batchSize = minOf(MAX_MEMBER_PROCESS_BATCH_SIZE, memberPairs.size)
      val batch = List(batchSize) { memberPairs.removeAt(Random.nextInt(memberPairs.size)) }
      try {
        // Randomly pick a batch to send to memberComparator
        memberComparator.process(batch)
      } catch (e: Exception) {
        log.error("Error processing batch: ${e.message}", e)
      }
      // Sleep for a bit between batches
      withContext(Dispatchers.IO) { Thread.sleep(DELAY_BETWEEN_BATCHES_MILLIS) }
    }

    val timeTaken = System.currentTimeMillis() - startTime
    log.info("Finished running MemberRefreshCronJob in $timeTaken ms")
    return "Finished running MemberRefreshCronJob in $timeTaken ms; <br/>\n" +
        "Members processed: ${dbMembers.joinToString { it.username + "<br/>\n" }}"
  }

  companion object {
    private const val MAX_MEMBER_PROCESS_BATCH_SIZE = 10
    private const val DELAY_BETWEEN_BATCHES_MILLIS = 50L
  }
}
