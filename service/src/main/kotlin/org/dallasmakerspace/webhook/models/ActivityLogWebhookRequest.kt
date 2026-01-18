package org.dallasmakerspace.webhook.models

import kotlinx.serialization.Serializable

/**
 * Request model for activity log webhook. Allows external systems to log events to the activity_log
 * table.
 */
@Serializable
data class ActivityLogWebhookRequest(
    val subjectUsername: String,
    val eventCode: Int,
    val actorUsername: String? = null,
    val attributes: String? = null
)
