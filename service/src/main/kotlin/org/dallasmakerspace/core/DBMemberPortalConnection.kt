package org.dallasmakerspace.core

import javax.inject.Inject
import javax.inject.Singleton
import org.jetbrains.exposed.sql.Database

/** Connection class for member portal database. */
@Singleton
class DBMemberPortalConnection @Inject constructor(val appConfig: AppConfig) {
  fun connect() {
    var dbUrl = appConfig.requireStringProperty("app.db.url")
    val dbUser = appConfig.requireStringProperty("app.db.user")
    val dbPassword = appConfig.requireStringProperty("app.db.password")

    // Add connection pool parameters to JDBC URL (if not already present)
    if (!dbUrl.contains("pool")) {
      val separator = if (dbUrl.contains("?")) "&" else "?"
      dbUrl +=
          "$separator" +
              "maxPoolSize=20&" +
              "minPoolSize=5&" +
              "maxIdleTime=600000&" +
              "serverTimezone=UTC&" +
              "autoReconnect=true&" +
              "connectTimeout=10000"
    }

    db = Database.connect(dbUrl, user = dbUser, password = dbPassword)
  }

  companion object {
    lateinit var db: Database
  }
}
