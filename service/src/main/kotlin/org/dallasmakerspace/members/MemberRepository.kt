package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.BaseRepository
import org.dallasmakerspace.members.db.ProfileDAO
import org.dallasmakerspace.members.db.ProfileTable
import org.dallasmakerspace.members.db.daoToProfileModel
import org.dallasmakerspace.members.db.suspendTransaction
import org.dallasmakerspace.models.DMSMember

/**
 * Manages member data. Fetches and updates member data. Contains validation and orchestration logic
 * for updating member data.
 */
class MemberRepository @Inject constructor(val appConfig: AppConfig) : BaseRepository(appConfig) {

  /**
   * Fetches member data from the database. If the member does not exist in the database, inserts a
   * new member.
   *
   * @param username The username of the member to fetch.
   * @return The member data.
   */
  suspend fun getMemberOrInsert(username: String): DMSMember {
    val existingMember = suspendTransaction {
      ProfileDAO.find { ProfileTable.username eq username }
          .firstOrNull()
          ?.let { daoToProfileModel(it) }
    }
    if (existingMember == null) {
      // Insert the member into the database.
      return daoToProfileModel(suspendTransaction { ProfileDAO.new {} })
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
          ProfileDAO.find { ProfileTable.username eq username }
              .firstOrNull()
              ?.let { daoToProfileModel(it) }
              ?: throw IllegalArgumentException("Member does not exist in DB: $username")
      existingMember.avatarUrl = member.avatarUrl
      existingMember.discourseUsername = member.discourseUsername
      existingMember.discourseAvatarUrl = member.discourseAvatarUrl
      existingMember.discordUserId = member.discordUserId
    }
  }
}
