package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.members.db.ProfileDAO
import org.dallasmakerspace.members.db.ProfileTable
import org.dallasmakerspace.members.db.daoToModel
import org.dallasmakerspace.members.db.suspendTransaction
import org.dallasmakerspace.models.DMSMember
import org.jetbrains.exposed.sql.Database

/**
 * Manages member data. Fetches and updates member data. Contains validation and orchestration logic
 * for updating member data.
 */
class MemberRepository @Inject constructor(val appConfig: AppConfig) {

  init {
    val dbUrl = appConfig.requireStringProperty("app.db.url")
    val dbUser = appConfig.requireStringProperty("app.db.user")
    val dbPassword = appConfig.requireStringProperty("app.db.password")
    Database.connect(dbUrl, user = dbUser, password = dbPassword)
  }

  /**
   * Fetches member data from the database. If the member does not exist in the database, inserts a
   * new member.
   *
   * @param username The username of the member to fetch.
   * @return The member data.
   */
  suspend fun getMemberOrInsert(username: String): DMSMember {
    val existingMember = suspendTransaction {
      ProfileDAO.find { ProfileTable.username eq username }.firstOrNull()?.let { daoToModel(it) }
    }
    if (existingMember == null) {
      // Insert the member into the database.
      return suspendTransaction { ProfileDAO.new(username) {} }.let { daoToModel(it) }
    } else {
      return existingMember
    }
  }

  /**
   * Updates member data in the database.
   *
   * @param username The username of the member to update.
   * @param member The member data to update.
   */
  suspend fun updateMember(username: String, member: DMSMember) {
    suspendTransaction {
      val existingMember =
          ProfileDAO.findById(username)
              ?: throw IllegalArgumentException("Member does not exist in DB: $username")
      existingMember.avatarUrl = member.avatarUrl
      existingMember.discourseUsername = member.discourseUsername
      existingMember.discourseAvatarUrl = member.discourseAvatarUrl
      existingMember.discordUserId = member.discordUserId
    }
  }
}
