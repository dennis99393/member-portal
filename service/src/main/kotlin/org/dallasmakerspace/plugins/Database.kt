package org.dallasmakerspace.plugins

import io.ktor.server.application.*
import org.dallasmakerspace.di.DaggerAppComponent

fun Application.configureDatabase() {
  val dbConnection = DaggerAppComponent.create().getDBConnection()
  dbConnection.connect()
  val dbMasterConnection = DaggerAppComponent.create().getDBMasterConnection()
  dbMasterConnection.connect()
}
