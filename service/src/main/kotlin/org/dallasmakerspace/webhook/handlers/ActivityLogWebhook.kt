package org.dallasmakerspace.webhook.handlers

import javax.inject.Inject
import kotlinx.serialization.json.Json
import org.dallasmakerspace.members.ActivityLogRepository
import org.dallasmakerspace.members.MemberRepository
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.ActivityLogSource
import org.dallasmakerspace.webhook.WebhookHandler
import org.dallasmakerspace.webhook.WebhookResult
import org.dallasmakerspace.webhook.models.ActivityLogWebhookRequest
import org.slf4j.LoggerFactory

/**
 * Webhook handler for activity log events. Allows external systems to log events to the
 * activity_log table with member username, event code, and optional JSON attributes.
 */
class ActivityLogWebhook
@Inject
constructor(
    private val memberRepository: MemberRepository,
    private val activityLogRepository: ActivityLogRepository,
) : WebhookHandler() {

  private val logger = LoggerFactory.getLogger(ActivityLogWebhook::class.java)
  private val json = Json { ignoreUnknownKeys = true }

  override fun getName() = "activity-log"

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handleWebhook(body: String): WebhookResult {
    return try {
      // Parse the webhook payload
      val request = json.decodeFromString<ActivityLogWebhookRequest>(body)

      // Validate and insert
      val validationResult = validateAndInsert(request)
      validationResult
    } catch (e: Exception) {
      logger.error("Error processing activity log webhook", e)
      WebhookResult(false, "Error processing webhook: ${e.message}")
    }
  }

  @Suppress("ReturnCount", "TooGenericExceptionCaught")
  private suspend fun validateAndInsert(request: ActivityLogWebhookRequest): WebhookResult {
    // Validate event code
    val event =
        ActivityLogEvent.entries.find { it.value == request.eventCode }
            ?: return WebhookResult(false, "Invalid event code: ${request.eventCode}")

    // Validate subject member exists
    val subjectMember =
        try {
          memberRepository.getMemberOrInsert(request.subjectUsername, null)
        } catch (e: Exception) {
          logger.error("Subject member not found: ${request.subjectUsername}", e)
          return WebhookResult(false, "Subject member not found: ${request.subjectUsername}")
        }

    // Validate actor member exists if provided
    var actorProfileId: Int? = null
    if (!request.actorUsername.isNullOrEmpty()) {
      try {
        val actorMember = memberRepository.getMemberOrInsert(request.actorUsername, null)
        actorProfileId = actorMember.id
      } catch (e: Exception) {
        logger.error("Actor member not found: ${request.actorUsername}", e)
        return WebhookResult(false, "Actor member not found: ${request.actorUsername}")
      }
    }

    // Validate JSON attributes if provided
    if (!request.attributes.isNullOrEmpty()) {
      try {
        Json.parseToJsonElement(request.attributes)
      } catch (e: Exception) {
        logger.error("Invalid JSON in attributes: ${e.message}")
        return WebhookResult(false, "Invalid JSON in attributes: ${e.message}")
      }
    }

    // Insert the activity log entry
    activityLogRepository.insertActivityLogEntry(
        source = ActivityLogSource.MEMBER_ACTIVITY_WEBHOOK,
        subjectProfileRowId = subjectMember.id,
        actorProfileRowId = actorProfileId,
        event = event,
        attributes = request.attributes)

    return WebhookResult(true, "Activity log entry created successfully")
  }
}
