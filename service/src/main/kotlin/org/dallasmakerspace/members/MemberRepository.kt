package org.dallasmakerspace.members

import org.dallasmakerspace.core.LoggerFactory
import javax.inject.Inject
import org.dallasmakerspace.members.db.ProfileDAO
import org.dallasmakerspace.members.db.ProfileTable
import org.dallasmakerspace.members.db.daoToProfileModel
import org.dallasmakerspace.members.db.suspendTransaction
import org.dallasmakerspace.models.DMSMember

/**
 * Manages member data. Fetches and updates member data. Contains validation and orchestration logic
 * for updating member data.
 */
class MemberRepository @Inject constructor(loggerFactory: LoggerFactory) {
  private val log = loggerFactory.create(javaClass)
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
    return if (existingMember == null) {
      log.info("Member not found in DB: $username; inserting new record.")
      daoToProfileModel(suspendTransaction { ProfileDAO.new(username) {} })
    } else {
      log.debug("Member found in DB: $username")
      existingMember
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
          ProfileDAO.find { ProfileTable.username eq username }.firstOrNull()
              ?: throw IllegalArgumentException("Member does not exist in DB: $username")
      existingMember.apply {
        avatarUrl = member.avatarUrl
        discourseUsername = member.discourseUsername
        discourseAvatarUrl = member.discourseAvatarUrl
        discordUserId = member.discordUserId
      }
    }
  }
}
