package org.dallasmakerspace.webhook.handlers

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dallasmakerspace.cron.SmartWaiverProcessQueueJob
import org.dallasmakerspace.smartwaiver.SmartWaiverRepository
import org.dallasmakerspace.webhook.WebhookHandler
import org.dallasmakerspace.webhook.WebhookResult
import org.dallasmakerspace.webhook.models.SmartWaiverWebhookRequest
import org.slf4j.LoggerFactory

private const val EVENT_NEW_WAIVER = "new-waiver"

class SmartWaiverWebhook
@Inject
constructor(
    private val smartWaiverRepository: SmartWaiverRepository,
    private val processQueueJob: SmartWaiverProcessQueueJob,
) : WebhookHandler() {

  private val logger = LoggerFactory.getLogger(SmartWaiverWebhook::class.java)
  private val json = Json { ignoreUnknownKeys = true }

  override fun getName() = "smartwaiver"

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handleWebhook(body: String): WebhookResult {
    return try {
      val request = json.decodeFromString<SmartWaiverWebhookRequest>(body)

      if (request.event != EVENT_NEW_WAIVER) {
        return WebhookResult(true, "Ignored event: ${request.event}")
      }

      val queued = smartWaiverRepository.insertWebhookQueueEntry(request.uniqueId, request.event)
      if (!queued) logger.warn("Duplicate webhook delivery ignored: ${request.uniqueId}")
      CoroutineScope(Dispatchers.IO).launch { processQueueJob.run() }
      WebhookResult(true, "Queued waiver: ${request.uniqueId}")
    } catch (e: Exception) {
      logger.error("Error queuing SmartWaiver webhook", e)
      WebhookResult(false, "Error queuing webhook: ${e.message}")
    }
  }
}
