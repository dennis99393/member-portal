package org.dallasmakerspace.smartwaiver

import org.jetbrains.exposed.sql.Table

@Suppress("MagicNumber")
object SmartWaiverWebhookQueueTable : Table("`dms-makermanager`.smartwaiver_webhook_queue") {
  val uniqueId = varchar("unique_id", 100)
  val event = varchar("event", 50)
  val receivedAt = varchar("received_at", 30)
  val processedAt = varchar("processed_at", 30).nullable()
  override val primaryKey = PrimaryKey(uniqueId)
}
