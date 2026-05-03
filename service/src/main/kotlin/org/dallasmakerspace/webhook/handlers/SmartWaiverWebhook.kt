package org.dallasmakerspace.webhook.handlers

import javax.inject.Inject
import kotlinx.serialization.json.Json
import org.dallasmakerspace.smartwaiver.ISmartwaiverApiClient
import org.dallasmakerspace.smartwaiver.SmartWaiverRepository
import org.dallasmakerspace.webhook.WebhookHandler
import org.dallasmakerspace.webhook.WebhookResult
import org.dallasmakerspace.webhook.models.SmartWaiverWebhookRequest
import org.slf4j.LoggerFactory

private const val EVENT_NEW_WAIVER = "new-waiver"

class SmartWaiverWebhook
@Inject
constructor(
    private val smartwaiverApiClient: ISmartwaiverApiClient,
    private val smartWaiverRepository: SmartWaiverRepository,
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

      val waiver = smartwaiverApiClient.getWaiver(request.uniqueId)
      smartWaiverRepository.insertSmartWaiver(waiver)
      WebhookResult(true, "Waiver stored: ${request.uniqueId}")
    } catch (e: Exception) {
      logger.error("Error processing SmartWaiver webhook", e)
      WebhookResult(false, "Error processing webhook: ${e.message}")
    }
  }
}
