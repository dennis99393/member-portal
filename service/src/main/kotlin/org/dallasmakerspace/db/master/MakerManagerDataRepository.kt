package org.dallasmakerspace.db.master

import java.sql.ResultSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import org.dallasmakerspace.core.DBMasterConnection
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// Data class to represent a user in MakerManager
data class MakerManagerUser(
    val username: String,
    val relatedUsername: String?,
    val makerManagerUserId: Int,
    val whmcsUserId: Int,
    val whmcsRealUserId: Int,
    val adActive: Boolean
)

@Singleton
class MakerManagerDataRepository @Inject constructor() {
  /**
   * Get related accounts for a list of usernames. This includes the account(s) being queried and
   * any related accounts like the primary account or addon accounts.
   *
   * @param usernames List of usernames to get related accounts for
   * @return Map of usernames to related accounts
   */
  suspend fun getRelatedAccountsMap(usernames: List<String>): Map<String, List<MakerManagerUser>> {
    val results = suspendTransaction {
      // Convert list of usernames to SQL-safe format
      val usernamesList = usernames.joinToString(",") { "'${it.replace("'", "''")}'" }

      val query =
          """
                    SELECT 
                        u1.username as main_username,
                        u2.username as related_username,
                        u2.user_id,
                        u2.whmcs_user_id,
                        u2.whmcs_real_user_id,
                        u2.ad_active
                    FROM `dms-makermanager`.users u1
                    INNER JOIN `dms-makermanager`.users u2 ON u1.whmcs_user_id = u2.whmcs_user_id
                    WHERE u1.username IN ($usernamesList)
                """
              .trimIndent()

      val makerManagerUsers = mutableListOf<MakerManagerUser>()

      this.exec(query) { resultSet: ResultSet ->
        while (resultSet.next()) {
          makerManagerUsers.add(
              MakerManagerUser(
                  username = resultSet.getString("main_username"),
                  relatedUsername = resultSet.getString("related_username"),
                  makerManagerUserId = resultSet.getInt("user_id"),
                  whmcsUserId = resultSet.getInt("whmcs_user_id"),
                  whmcsRealUserId = resultSet.getInt("whmcs_real_user_id"),
                  adActive = resultSet.getInt("ad_active") == 1))
        }
        makerManagerUsers
      } ?: emptyList()

      makerManagerUsers
    }

    // Convert the results to a map, maintaining the same return type
    return results.groupBy { it.username }
  }
}

// Keep the transaction helper function
suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block, db = DBMasterConnection.db)
