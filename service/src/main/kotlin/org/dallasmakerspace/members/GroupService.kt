package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember
import org.dallasmakerspace.routing.Groups

class GroupService
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val activeDirectoryService: ActiveDirectoryService,
) {
  private val log = loggerFactory.create(javaClass)

  fun getAllGroups(): List<DMSGroup> {
    // Get all groups directly from Active Directory
    val adGroups = activeDirectoryService.getAllGroups()

    return adGroups.map { adGroup ->
      DMSGroup(
          name = adGroup.cn,
          description = adGroup.description,
          distinguishedName = adGroup.distinguishedName,
          objectGuid = adGroup.objectGuid,
          members = emptyList(), // Don't load members for performance
          membersListIncomplete = adGroup.membersListIncomplete,
          administrators = adGroup.administrators,
          nestedGroups = adGroup.nestedGroups)
    }
  }

  fun getGroup(groupslug: String): DMSGroup {
    val groupname = Groups.getNameFromSlug(groupslug)
    val adGroup = activeDirectoryService.getGroup(groupname)
    return convertAdGroupToDMSGroup(adGroup)
  }

  fun getMultipleGroups(groupslugs: List<String>): List<DMSGroup> {
    val groupnames = groupslugs.map { Groups.getNameFromSlug(it) }
    val adGroups = activeDirectoryService.getMultipleGroups(groupnames)
    return adGroups.map { convertAdGroupToDMSGroup(it) }
  }

  private fun convertAdGroupToDMSGroup(adGroup: org.dallasmakerspace.activedirectory.ADGroup): DMSGroup {
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
                            members = emptyList())
                      })
            },
        membersListIncomplete = adGroup.membersListIncomplete,
        administrators = adGroup.administrators,
        nestedGroups = adGroup.nestedGroups)
  }

  private fun getNormalizedPhoneNumber(
      telephoneNumber: String?,
      usernameForLogging: String
  ): String? {
    if (telephoneNumber.isNullOrBlank() || telephoneNumber == "null") {
      return null
    }
    return try {
      val pnu = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()
      val pn = pnu.parse(telephoneNumber, "US")
      pnu.format(pn, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
    } catch (e: com.google.i18n.phonenumbers.NumberParseException) {
      log.warn("Failed to parse phone number: $telephoneNumber for user: $usernameForLogging")
      null
    }
  }

  private fun calculateMemberSince(whenCreated: String?): kotlinx.datetime.Instant? {
    if (whenCreated == null) {
      return null
    }
    // Parse the whenCreated string from a format like "20220910163620.0Z" to an Instant. Instant
    // expects this format "2022-09-10T16:36:20Z"
    val instant =
        kotlinx.datetime.Instant.parse(
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
}
