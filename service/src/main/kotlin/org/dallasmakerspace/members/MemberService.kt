package org.dallasmakerspace.members

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toKotlinLocalDate
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.db.master.MakerManagerDataService
import org.dallasmakerspace.db.master.WhmcsDataService
import org.dallasmakerspace.discourse.DiscourseAvatarService
import org.dallasmakerspace.discourse.DiscourseService
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember
import org.dallasmakerspace.routing.Groups
import org.dallasmakerspace.voterregistration.VoterRegistrationManager
import javax.inject.Inject

@Suppress("LongParameterList")
class MemberService
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val discourseService: DiscourseService,
    private val discourseAvatarService: DiscourseAvatarService,
    private val memberRepository: MemberRepository,
    private val whmcsDataService: WhmcsDataService,
    private val activityLogService: ActivityLogService,
    private val makerManagerDataService: MakerManagerDataService,
    private val activeDirectoryService: ActiveDirectoryService,
    private val voterRegistrationManager: VoterRegistrationManager,
    private val groupHistoryRepository: GroupHistoryRepository,
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun getMembersLoggedInDays(days: Int): List<DMSMember> {
    val adMembers = activeDirectoryService.getMembersByLoggedInDays(days)
    val dbMembers = memberRepository.getAllMembers()
    log.debug("Found ${adMembers.size} members logged in the last $days days")
    return adMembers.map { adMember ->
      DMSMember(
          id = -1,
          username = adMember.sAMAccountName,
          displayName = adMember.displayName,
          enabled = adMember.enabled,
          personalEmail = adMember.mail,
          phoneNumber = adMember.telephoneNumber,
          badgeNumber = adMember.employeeID,
          discourseUsername =
              dbMembers.find { it.username == adMember.sAMAccountName }?.discourseUsername,
          memberSince = calculateMemberSince(adMember.whenCreated),
          groups = emptyList(),
      )
    }
  }

  suspend fun getMemberByUsername(username: String): DMSMember {
    // Fetch member from active directory.
    val adMember = activeDirectoryService.getMemberByUsername(username)

    // Get related accounts from MakerManager
    val relatedAccounts = makerManagerDataService.getAccountInfoMap(listOf(username))

    val accountInfo = relatedAccounts[username]
    if (accountInfo != null) {
      val account =
          if (accountInfo.isPrimaryAccount) accountInfo.primaryAccount
          else accountInfo.addonAccounts.find { it.username == username }

      account?.let {
        val whmcsId = it.whmcsId
        // Get the account status from WHMCS
        val accountStatus = whmcsDataService.getAccountInfoMap(listOf(whmcsId))[whmcsId]
        if (accountStatus != null) {
          accountInfo.wasActivePast90Days = accountStatus.wasActiveInRange
          accountInfo.lastInactiveDate = accountStatus.lastInactiveDate?.toKotlinLocalDate()
          accountInfo.regDate =
              if (accountInfo.isPrimaryAccount)
                  whmcsDataService.getAccountRegdate(whmcsId)?.toKotlinLocalDate()
              else
                  calculateMemberSince(adMember.whenCreated)?.let { createdInstant ->
                    java.time.LocalDate.ofEpochDay(createdInstant.epochSeconds / 86_400)
                        .toKotlinLocalDate()
                  }
        }
      }
    }

    val dbMember = memberRepository.getMemberOrInsert(username, adMember.enabled)
    // TODO: compare DB and AD members and notify observers of any changes.
    dbMember.firstName = adMember.givenName
    dbMember.lastName = adMember.sn
    dbMember.displayName = adMember.displayName
    dbMember.personalEmail = adMember.mail
    dbMember.phoneNumber =
        getNormalizedPhoneNumber(adMember.telephoneNumber, adMember.sAMAccountName)
    dbMember.badgeNumber = adMember.employeeID
    dbMember.enabled = adMember.enabled
    dbMember.memberSince = calculateMemberSince(accountInfo?.regDate)
    dbMember.groups =
        adMember.groups.map { group ->
          DMSGroup(
              name = group.cn,
              description = null,
              distinguishedName = group.distinguishedName,
              objectGuid = group.objectGuid,
              membersListIncomplete = false,
              members = null,
          )
        }
    dbMember.accountInfo = relatedAccounts[username]

    // Refresh discourse avatar URL if member has a discourse username
    dbMember.discourseAvatarUrl = refreshDiscourseAvatar(dbMember)

    return dbMember
  }

  /**
   * Refreshes the discourse avatar URL for a member if they have a discourse username. Implements
   * smart caching and fallback mechanisms.
   *
   * @param member The member to refresh avatar for
   * @return The updated avatar URL, or the existing one if refresh fails/is skipped
   */
  private suspend fun refreshDiscourseAvatar(member: DMSMember): String? {
    val discourseUsername = member.discourseUsername

    // Return existing avatar URL if no discourse username
    if (discourseUsername.isNullOrBlank()) {
      log.debug("Member ${member.username} has no discourse username, skipping avatar refresh")
      return member.discourseAvatarUrl
    }

    try {
      // Attempt to refresh avatar URL using the avatar service
      // The service has its own caching to prevent excessive API calls
      val refreshedAvatarUrl = discourseAvatarService.refreshAvatarUrl(discourseUsername)

      if (refreshedAvatarUrl != null) {
        // Successfully refreshed - update database if URL changed
        if (refreshedAvatarUrl != member.discourseAvatarUrl) {
          val updateSuccess =
              memberRepository.updateDiscourseAvatarUrl(member.username, refreshedAvatarUrl)
          if (updateSuccess) {
            log.debug(
                "Successfully updated discourse avatar URL for ${member.username}: $refreshedAvatarUrl"
            )
            return refreshedAvatarUrl
          } else {
            log.warn(
                "Failed to update discourse avatar URL in database for ${member.username}, using existing URL"
            )
            return member.discourseAvatarUrl // Fallback to existing URL
          }
        } else {
          log.debug("Discourse avatar URL unchanged for ${member.username}")
          return member.discourseAvatarUrl
        }
      } else {
        // Avatar service returned null (likely due to caching or API failure)
        log.debug("Avatar refresh returned null for ${member.username}, using existing URL")
        return member.discourseAvatarUrl // Fallback to existing URL
      }
    } catch (e: Exception) {
      // Error during avatar refresh - fallback to existing URL
      log.warn("Failed to refresh discourse avatar for ${member.username}: ${e.message}")
      return member.discourseAvatarUrl // Fallback to existing URL
    }
  }

  suspend fun getMemberByBadgeNumber(badgeNumber: String): DMSMember {
    // Fetch member from active directory.
    val adMember = activeDirectoryService.getMemberByBadgeNumber(badgeNumber)

    val dbMember = memberRepository.getMemberOrInsert(adMember.sAMAccountName, adMember.enabled)
    // TODO: compare DB and AD members and notify observers of any changes.
    dbMember.firstName = adMember.givenName
    dbMember.lastName = adMember.sn
    dbMember.displayName = adMember.displayName
    dbMember.personalEmail = adMember.mail
    dbMember.phoneNumber =
        getNormalizedPhoneNumber(adMember.telephoneNumber, adMember.sAMAccountName)
    dbMember.badgeNumber = adMember.employeeID
    dbMember.enabled = adMember.enabled
    dbMember.memberSince = calculateMemberSince(adMember.whenCreated)
    dbMember.groups =
        adMember.groups.map { group ->
          DMSGroup(
              name = group.cn,
              description = null,
              distinguishedName = group.distinguishedName,
              objectGuid = group.objectGuid,
              membersListIncomplete = false,
              members = null,
          )
        }
    return dbMember
  }

  private fun getNormalizedPhoneNumber(
      telephoneNumber: String?,
      usernameForLogging: String,
  ): String? {
    if (telephoneNumber.isNullOrBlank() || telephoneNumber == "null") {
      return null
    }
    val pnE164: String? =
        try {
          val pnu: PhoneNumberUtil = PhoneNumberUtil.getInstance()
          val pn: Phonenumber.PhoneNumber = pnu.parse(telephoneNumber, "US")
          pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } catch (e: NumberParseException) {
          log.warn("Failed to parse phone number: $telephoneNumber for user: $usernameForLogging")
          null
        }
    return pnE164
  }

  /** Calculates the memberSince date from the whenCreated string. */
  @Deprecated(
      "Use org.dallasmakerspace.members.MemberService.calculateMemberSince(java.time.LocalDate) instead"
  )
  @Suppress("MagicNumber")
  private fun calculateMemberSince(whenCreated: String?): Instant? {
    if (whenCreated == null) {
      return null
    }
    // Parse the whenCreated string from a format like "20220910163620.0Z" to an Instant. Instant
    // expects this format "2022-09-10T16:36:20Z"
    val instant =
        Instant.parse(
            whenCreated.substring(0, 4) +
                "-" +
                whenCreated.substring(4, 6) +
                "-" +
                whenCreated.substring(6, 8) +
                "T" +
                whenCreated.substring(8, 10) +
                ":" +
                whenCreated.substring(10, 12) +
                ":" +
                whenCreated.substring(12, 14) +
                "Z"
        )
    return instant
  }

  /** Calculates the memberSince date from a LocalDate. */
  private fun calculateMemberSince(localDate: LocalDate?): Instant? {
    if (localDate == null) {
      return null
    }
    // Convert LocalDate to Instant at the start of the day in UTC
    return localDate.atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
  }

  suspend fun updateMember(username: String, memberFromApi: DMSMember) {
    // Fetch member from active directory.
    val adMember = activeDirectoryService.getMemberByUsername(username)
    // If member is not active in AD then throw an exception.
    check(adMember.enabled) {
      "Member is not active in AD: $username, we should not update this record!"
    }
    val dbMember = memberRepository.getMemberOrInsert(username, adMember.enabled)
    var propertiesUpdated = false
    // Figure out which properties have been updated by comparing dbMember and memberFromApi.
    if (dbMember.avatarUrl != memberFromApi.avatarUrl) {
      log.info(
          "Avatar URL updated for $username; Old URL: ${dbMember.avatarUrl}; New URL: ${memberFromApi.avatarUrl}"
      )
      propertiesUpdated = true
    }
    if (dbMember.discourseUsername != memberFromApi.discourseUsername) {
      log.info(
          "Discourse username updated for $username; Old username: ${dbMember.discourseUsername}; " +
              "New username: ${memberFromApi.discourseUsername}"
      )
      propertiesUpdated = true
      if (memberFromApi.discourseUsername == null) {
        unlinkDiscourse(dbMember)
      } else {
        linkDiscourse(memberFromApi)
      }
    }
    if (dbMember.discordUserId != memberFromApi.discordUserId) {
      log.info(
          "Discord user ID updated for $username; Old ID: ${dbMember.discordUserId}; " +
              "New ID: ${memberFromApi.discordUserId}"
      )
      propertiesUpdated = true
    }
    if (propertiesUpdated) {
      // Update the member in the database.
      log.info("Updating member in the database: $username")
      memberRepository.updateMember(username, memberFromApi)
    }
  }

  private suspend fun linkDiscourse(memberFromApi: DMSMember) {
    // Add the member to the discourse group.
    discourseService.addUserToDmsMembersV2Group(
        listOf(requireNotNull(memberFromApi.discourseUsername))
    )
    activityLogService.insertActivityLogEntry(
        subjectUsername = memberFromApi.username,
        event = ActivityLogEvent.LINK_DISCOURSE,
    )
  }

  private suspend fun unlinkDiscourse(dbMember: DMSMember) {
    // Remove the member from the discourse group.
    discourseService.removeUserFromDmsMembersV2Group(
        listOf(requireNotNull(dbMember.discourseUsername))
    )
    activityLogService.insertActivityLogEntry(
        subjectUsername = dbMember.username,
        event = ActivityLogEvent.UNLINK_DISCOURSE,
    )
  }

  suspend fun getAllMembersWithProfiles(): List<DMSMember> {
    return memberRepository.getAllMembers()
  }

  suspend fun getAllMembers(): List<DMSMember> {
    val totalStartNs = System.nanoTime()

    // Get all users from MakerManager
    val mmStartNs = System.nanoTime()
    val makerManagerUsers = makerManagerDataService.getAllUsers()
    val mmMs = (System.nanoTime() - mmStartNs) / 1_000_000

    // Get all members from the repository to get discourse usernames
    val dbStartNs = System.nanoTime()
    val dbMembers = memberRepository.getAllMembers()
    val dbMs = (System.nanoTime() - dbStartNs) / 1_000_000

    // Create a map of usernames to discourse usernames for quick lookup and combine data
    val combineStartNs = System.nanoTime()
    val discourseUsernameMap = dbMembers.associateBy({ it.username }, { it.discourseUsername })
    val result =
        makerManagerUsers.map { mmUser ->
          DMSMember(
              id = mmUser.makerManagerId,
              username = mmUser.username,
              firstName = mmUser.firstName,
              lastName = mmUser.lastName,
              displayName =
                  "${mmUser.firstName ?: ""} ${mmUser.lastName ?: ""}".trim().takeIf {
                    it.isNotEmpty()
                  },
              personalEmail = mmUser.email,
              phoneNumber = mmUser.phone,
              badgeNumber = mmUser.badgeNumber,
              enabled = mmUser.adActive,
              discourseUsername = discourseUsernameMap[mmUser.username],
              memberSince = null, // Not available in MakerManager query
              groups = emptyList(),
              accountInfo = null,
          )
        }
    val combineMs = (System.nanoTime() - combineStartNs) / 1_000_000

    val totalMs = (System.nanoTime() - totalStartNs) / 1_000_000
    log.info(
        "getAllMembers timings: total=${totalMs}ms, makerManager=${mmMs}ms, " +
            "memberRepository=${dbMs}ms, combine=${combineMs}ms"
    )
    return result
  }

  suspend fun getGroup(groupslug: String): DMSGroup {
    val groupname = Groups.getNameFromSlug(groupslug)
    val adGroup = activeDirectoryService.getGroup(groupname)
    // Get the members from DB so we can include their discourse usernames and other details.
    val dbMembers = memberRepository.getAllMembers()
    // Get group history
    val groupHistory = groupHistoryRepository.getGroupHistoryByName(groupname)
    return DMSGroup(
        name = adGroup.cn,
        description = adGroup.description,
        distinguishedName = adGroup.distinguishedName,
        objectGuid = adGroup.objectGuid,
        members =
            adGroup.members.map {
              // Find corresponding DB member
              val dbMember = dbMembers.find { db -> db.username == it.sAMAccountName }
              DMSMember(
                  -1,
                  it.sAMAccountName,
                  firstName = it.givenName,
                  lastName = it.sn,
                  displayName = it.displayName,
                  avatarUrl = dbMember?.discourseAvatarUrl,
                  discourseUsername = dbMember?.discourseUsername,
                  personalEmail = it.mail,
                  phoneNumber = getNormalizedPhoneNumber(it.telephoneNumber, it.sAMAccountName),
                  badgeNumber = it.employeeID,
                  enabled = it.enabled,
                  memberSince = calculateMemberSince(it.whenCreated),
                  groups =
                      it.groups.map { group ->
                        DMSGroup(
                            name = group.cn,
                            description = null,
                            distinguishedName = group.distinguishedName,
                            objectGuid = group.objectGuid,
                            membersListIncomplete = false,
                            members = null,
                        )
                      },
              )
            },
        membersListIncomplete = adGroup.membersListIncomplete,
        administrators = adGroup.administrators,
        history = groupHistory,
    )
  }

  suspend fun addMembersToGroup(memberUsernames: List<String>, groupslug: String) {
    val groupname = Groups.getNameFromSlug(groupslug)
    activeDirectoryService.addUsersToGroup(memberUsernames, groupname)
    if (groupname == voterRegistrationManager.getVotingMembersGroupName()) {
      memberUsernames.forEach { username ->
        activityLogService.insertActivityLogEntry(
            subjectUsername = username,
            event = ActivityLogEvent.ADD_TO_VOTING_MEMBERS_GROUP,
        )
      }
    }
  }

  suspend fun removeMembersToGroup(memberUsernames: List<String>, groupslug: String) {
    val groupname = Groups.getNameFromSlug(groupslug)
    activeDirectoryService.removeUsersFromGroup(memberUsernames, groupname)
    if (groupname == voterRegistrationManager.getVotingMembersGroupName()) {
      memberUsernames.forEach { username ->
        activityLogService.insertActivityLogEntry(
            subjectUsername = username,
            event = ActivityLogEvent.REMOVE_FROM_VOTING_MEMBERS_GROUP,
        )
      }
    }
  }
}
