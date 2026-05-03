package org.dallasmakerspace.webhook.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmartWaiverWebhookRequest(
    @SerialName("unique_id") val uniqueId: String,
    val event: String,
)
