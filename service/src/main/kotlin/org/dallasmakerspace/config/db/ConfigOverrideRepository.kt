package org.dallasmakerspace.config.db

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.members.db.suspendTransaction
import org.jetbrains.exposed.sql.SortOrder

class ConfigOverrideRepository @Inject constructor(loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)

  suspend fun getCurrent(key: String): ConfigOverrideDAO? = suspendTransaction {
    ConfigOverrideDAO.find { ConfigOverrideTable.configKey eq key }
        .orderBy(ConfigOverrideTable.changedAt to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
  }

  suspend fun getHistory(key: String): List<ConfigOverrideDAO> = suspendTransaction {
    ConfigOverrideDAO.find { ConfigOverrideTable.configKey eq key }
        .orderBy(ConfigOverrideTable.changedAt to SortOrder.DESC)
        .toList()
  }

  suspend fun getAllCurrent(): Map<String, ConfigOverrideDAO> = suspendTransaction {
    ConfigOverrideDAO.all()
        .orderBy(ConfigOverrideTable.changedAt to SortOrder.DESC)
        .toList()
        .distinctBy { it.configKey }
        .associateBy { it.configKey }
  }

  suspend fun append(
      key: String,
      value: String,
      type: String,
      changedBy: String,
      reason: String,
      isReset: Boolean = false,
  ): ConfigOverrideDAO = suspendTransaction {
    ConfigOverrideDAO.new {
      configKey = key
      configValue = value
      configType = type
      this.changedBy = changedBy
      changeReason = reason
      changedAt = LocalDateTime.now(ZoneOffset.UTC)
      this.isReset = isReset
    }
  }
}
