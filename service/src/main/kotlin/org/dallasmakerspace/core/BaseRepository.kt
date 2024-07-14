package org.dallasmakerspace.core

import org.jetbrains.exposed.sql.Database

abstract class BaseRepository(appConfig: AppConfig) {
  init {
    val dbUrl = appConfig.requireStringProperty("app.db.url")
    val dbUser = appConfig.requireStringProperty("app.db.user")
    val dbPassword = appConfig.requireStringProperty("app.db.password")
    Database.connect(dbUrl, user = dbUser, password = dbPassword)
  }
}
