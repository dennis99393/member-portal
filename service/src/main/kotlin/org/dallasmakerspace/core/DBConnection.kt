package org.dallasmakerspace.core

import javax.inject.Inject
import javax.inject.Singleton
import org.jetbrains.exposed.sql.Database

@Singleton
class DBConnection @Inject constructor(val appConfig: AppConfig) {
  fun connect() {
    val dbUrl = appConfig.requireStringProperty("app.db.url")
    val dbUser = appConfig.requireStringProperty("app.db.user")
    val dbPassword = appConfig.requireStringProperty("app.db.password")
    Database.connect(dbUrl, user = dbUser, password = dbPassword)
  }
}
