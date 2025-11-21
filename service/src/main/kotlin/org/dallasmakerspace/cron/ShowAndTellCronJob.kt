package org.dallasmakerspace.cron

import dagger.Reusable
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.core.logging.AppInMemoryLogger
import org.dallasmakerspace.discourse.IDiscourseApiClient

/**
 * This cron job manages the monthly "Show and Tell" posts in Discourse. It creates a new globally
 * pinned post for the current month and archives (unpins and locks) the previous month's post.
 */
@Reusable
class ShowAndTellCronJob
@Inject
constructor(loggerFactory: LoggerFactory, private val discourseApiClient: IDiscourseApiClient) :
    CronJob<ShowAndTellCronJobParams>(ShowAndTellCronJobParams::class, loggerFactory) {

  companion object {
    private const val SHOW_AND_TELL_CATEGORY_ID = 49
  }

  @Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
  override suspend fun run(params: ShowAndTellCronJobParams): String {
    log.info(
        "**************************************************************************************"
    )
    log.info("Running Show and Tell Cron Job; params: $params")
    log.info(
        "**************************************************************************************"
    )
    val startTime = System.currentTimeMillis()

    try {
      val currentMonth = getCurrentMonthName()
      val previousMonth = getPreviousMonthName()
      log.info("Current month: $currentMonth, Previous month: $previousMonth")

      handleCurrentMonthPost(currentMonth, params)
      handlePreviousMonthPost(previousMonth, params)
    } catch (e: Exception) {
      log.error("Error running ShowAndTellCronJob: ${e.message}", e)
    }

    val timeTaken = System.currentTimeMillis() - startTime
    log.info("Finished running ShowAndTellCronJob in $timeTaken ms")

    val logBuffer = (log as? AppInMemoryLogger)?.getLog() ?: ""
    (log as? AppInMemoryLogger)?.clear()
    return logBuffer
  }

  /** Handle creating and pinning the current month's post. */
  @Suppress("TooGenericExceptionCaught")
  private suspend fun handleCurrentMonthPost(
      currentMonth: String,
      params: ShowAndTellCronJobParams,
  ) {
    val currentMonthTitle = "Show and Tell $currentMonth"
    val existingCurrentMonthPost = findTopicByTitle(currentMonthTitle, SHOW_AND_TELL_CATEGORY_ID)

    if (existingCurrentMonthPost != null) {
      log.info(
          "Post for current month already exists (Topic ID: ${existingCurrentMonthPost.id}). Skipping creation."
      )
      return
    }

    if (params.isRunningInShadowMode) {
      log.info(
          "Shadow mode: Would create post '$currentMonthTitle' in category $SHOW_AND_TELL_CATEGORY_ID"
      )
      return
    }

    val postBody = generatePostBody(currentMonth)
    val createdPost =
        discourseApiClient.createPost(currentMonthTitle, postBody, SHOW_AND_TELL_CATEGORY_ID)
    log.info("Created new post: $currentMonthTitle (Topic ID: ${createdPost.topicId})")

    try {
      discourseApiClient.pinTopic(createdPost.topicId, pinned = true, pinGlobally = true)
      log.info("Pinned topic ${createdPost.topicId} globally")
    } catch (e: Exception) {
      log.error("Failed to pin new topic ${createdPost.topicId}: ${e.message}", e)
    }
  }

  /** Handle unpinning and locking the previous month's post. */
  @Suppress("TooGenericExceptionCaught")
  private suspend fun handlePreviousMonthPost(
      previousMonth: String,
      params: ShowAndTellCronJobParams,
  ) {
    val previousMonthTitle = "Show and Tell $previousMonth"
    val existingPreviousMonthPost = findTopicByTitle(previousMonthTitle, SHOW_AND_TELL_CATEGORY_ID)

    if (existingPreviousMonthPost == null) {
      log.info("No previous month post found with title '$previousMonthTitle'. Nothing to archive.")
      return
    }

    if (params.isRunningInShadowMode) {
      log.info(
          "Shadow mode: Would unpin and lock previous month topic ${existingPreviousMonthPost.id}"
      )
      return
    }

    try {
      discourseApiClient.pinTopic(existingPreviousMonthPost.id, pinned = false, pinGlobally = true)
      log.info("Unpinned previous month topic ${existingPreviousMonthPost.id}")

      discourseApiClient.updateTopicStatus(existingPreviousMonthPost.id, "closed", enabled = true)
      log.info("Locked previous month topic ${existingPreviousMonthPost.id}")
    } catch (e: Exception) {
      log.error(
          "Failed to archive previous month topic ${existingPreviousMonthPost.id}: ${e.message}",
          e,
      )
    }
  }

  /**
   * Get the current month name in "MMMM yyyy" format (e.g., "January 2025"). Uses Dallas timezone
   * (America/Chicago).
   */
  private fun getCurrentMonthName(): String {
    val now = YearMonth.now(ZoneId.of("America/Chicago"))
    return now.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
  }

  /**
   * Get the previous month name in "MMMM yyyy" format (e.g., "December 2024"). Uses Dallas timezone
   * (America/Chicago).
   */
  private fun getPreviousMonthName(): String {
    val lastMonth = YearMonth.now(ZoneId.of("America/Chicago")).minusMonths(1)
    return lastMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
  }

  /** Generate the body content for the Show and Tell post. */
  private fun generatePostBody(month: String): String {
    return """
    **Please post a picture & description in this thread of anything you are working on this month at the 'Space.**

    Projects happen at DMS happen all the time, but most of our fellow members don't get to see our cool stuff! Post here to share the interesting things we are doing @ Dallas Makerspace this month!
    :heavy_check_mark: large craft project
    :heavy_check_mark: small CNC router project 
    :heavy_check_mark: building a coffee table 
    :heavy_check_mark: scientific research
    and so much more! 

    Posting here helps not only promote Dallas Makerspace, but could inspire others to make something. This also helps PR enrich social media content in our blog, Instagram, TikTok, or other social media (_with attribution to each maker of course_).

    <br>

    :bulb: **NOTE: Please try to include the following on each post, to help make for better social media content!**

    * a QUALITY photo
    * a note about WHAT you've made
    * WHO you are (_for attribution_)
    * HOW you've made it
    * and  WHY it was made
    """
        .trimIndent()
  }

  /**
   * Find a topic by exact title match in the given category. Returns null if not found or if
   * multiple matches exist.
   */
  @Suppress("TooGenericExceptionCaught")
  private suspend fun findTopicByTitle(
      title: String,
      categoryId: Int,
  ): org.dallasmakerspace.discourse.DiscourseTopicSearchResult? {
    return try {
      val searchResults = discourseApiClient.searchTopics(title, categoryId)
      val matchingTopics =
          searchResults.topics.filter { it.title == title && it.categoryId == categoryId }

      when {
        matchingTopics.isEmpty() -> {
          log.debug("No topic found with title: $title")
          null
        }
        matchingTopics.size == 1 -> {
          log.debug("Found topic with title '$title': ${matchingTopics[0].id}")
          matchingTopics[0]
        }
        else -> {
          log.warn(
              "Found multiple topics with title '$title'. Using first match: ${matchingTopics[0].id}"
          )
          matchingTopics[0]
        }
      }
    } catch (e: Exception) {
      log.error("Error searching for topic '$title': ${e.message}", e)
      null
    }
  }
}
