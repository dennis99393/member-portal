package org.dallasmakerspace.webhook

import javax.inject.Inject

/**
 * Router class for webhook endpoints. Routes incoming webhook requests to the appropriate handler
 * based on the path.
 */
class WebhookRouter
@Inject
constructor(webhookHandlerSet: Set<@JvmSuppressWildcards WebhookHandler>) {
  private val handlerMap: Map<String, WebhookHandler> =
      webhookHandlerSet.associateBy { it.getName() }

  /**
   * Routes the webhook request to the appropriate handler.
   *
   * @param path The path part after /webhook/
   * @param body The request body as string
   * @return Result of the webhook processing
   */
  suspend fun route(path: String, body: String): WebhookResult {
    val handler =
        handlerMap[path] ?: return WebhookResult(false, "No handler found for path: $path")
    return handler.handleWebhook(body)
  }
}

/**
 * Abstract class representing a webhook handler. Each handler has a unique name and must implement
 * the `handleWebhook` method.
 */
abstract class WebhookHandler {

  /** Returns the name of the webhook handler, used for routing. */
  abstract fun getName(): String

  /**
   * Processes the webhook request body.
   *
   * @param body The request body as string
   * @return A WebhookResult indicating success or failure
   */
  abstract suspend fun handleWebhook(body: String): WebhookResult
}

/** Data class representing the result of a webhook processing. */
data class WebhookResult(val success: Boolean, val message: String)
