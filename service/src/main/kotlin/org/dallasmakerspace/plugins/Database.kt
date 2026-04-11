package org.dallasmakerspace.plugins

import io.ktor.server.application.*
import org.dallasmakerspace.config.db.ConfigOverrideTable
import org.dallasmakerspace.core.DBMemberPortalConnection
import org.dallasmakerspace.di.DaggerAppComponent
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
  val dbConnection = DaggerAppComponent.create().getDBConnection()
  dbConnection.connect()
  val dbMasterConnection = DaggerAppComponent.create().getDBMasterConnection()
  dbMasterConnection.connect()
  transaction(DBMemberPortalConnection.db) {
    SchemaUtils.createMissingTablesAndColumns(ConfigOverrideTable)
  }
}
