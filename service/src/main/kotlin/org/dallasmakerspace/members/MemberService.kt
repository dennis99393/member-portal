package org.dallasmakerspace.members

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import javax.inject.Inject
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinLocalDate
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.db.master.MakerManagerDataService
import org.dallasmakerspace.db.master.WhmcsDataService
import org.dallasmakerspace.discourse.DiscourseService
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember
import org.dallasmakerspace.routing.Groups

@Suppress("LongParameterList")
class MemberService
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val discourseService: DiscourseService,
    private val memberRepository: MemberRepository,
    private val whmcsDataService: WhmcsDataService,
    private val activityLogService: ActivityLogService,
    private val makerManagerDataService: MakerManagerDataService,
    private val activeDirectoryService: ActiveDirectoryService,
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
    if (accountInfo != null && accountInfo.isPrimaryAccount) {
      accountInfo.primaryAccount?.let { account ->
        val whmcsId = account.whmcsId
        // Get the account status from WHMCS
        val accountStatus = whmcsDataService.getAccountInfoMap(listOf(whmcsId))[whmcsId]
        if (accountStatus != null) {
          accountInfo.wasActivePast90Days = accountStatus.wasActiveInRange
          accountInfo.lastInactiveDate = accountStatus.lastInactiveDate?.toKotlinLocalDate()
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
    dbMember.memberSince = calculateMemberSince(adMember.whenCreated)
    dbMember.groups =
        adMember.groups.map { group ->
          DMSGroup(
              name = group.cn,
              description = null,
              distinguishedName = group.distinguishedName,
              objectGuid = group.objectGuid,
              membersListIncomplete = false,
              members = null)
        }
    dbMember.accountInfo = relatedAccounts[username]
    return dbMember
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
              members = null)
        }
    return dbMember
  }

  private fun getNormalizedPhoneNumber(
      telephoneNumber: String?,
      usernameForLogging: String
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
                "Z")
    return instant
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
          "Avatar URL updated for $username; Old URL: ${dbMember.avatarUrl}; New URL: ${memberFromApi.avatarUrl}")
      propertiesUpdated = true
    }
    if (dbMember.discourseUsername != memberFromApi.discourseUsername) {
      log.info(
          "Discourse username updated for $username; Old username: ${dbMember.discourseUsername}; " +
              "New username: ${memberFromApi.discourseUsername}")
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
              "New ID: ${memberFromApi.discordUserId}")
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
        listOf(requireNotNull(memberFromApi.discourseUsername)))
    activityLogService.insertActivityLogEntry(
        subjectUsername = memberFromApi.username, event = ActivityLogEvent.LINK_DISCOURSE)
  }

  private suspend fun unlinkDiscourse(dbMember: DMSMember) {
    // Remove the member from the discourse group.
    discourseService.removeUserFromDmsMembersV2Group(
        listOf(requireNotNull(dbMember.discourseUsername)))
    activityLogService.insertActivityLogEntry(
        subjectUsername = dbMember.username, event = ActivityLogEvent.UNLINK_DISCOURSE)
  }

  suspend fun getAllMembers(): List<DMSMember> {
    return memberRepository.getAllMembers()
  }

  fun getGroup(groupslug: String): DMSGroup {
    val groupname = Groups.getNameFromSlug(groupslug)
    val adGroup = activeDirectoryService.getGroup(groupname)
    return DMSGroup(
        name = adGroup.cn,
        description = adGroup.description,
        distinguishedName = adGroup.distinguishedName,
        objectGuid = adGroup.objectGuid,
        members =
            adGroup.members.map {
              DMSMember(
                  -1,
                  it.sAMAccountName,
                  firstName = it.givenName,
                  lastName = it.sn,
                  displayName = it.displayName,
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
                            members = null)
                      })
            },
        membersListIncomplete = adGroup.membersListIncomplete)
  }
}
