package org.dallasmakerspace.members

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import javax.inject.Inject
import kotlinx.datetime.Instant
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.core.Log
import org.dallasmakerspace.discourse.DiscourseService
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember

class MemberService
@Inject
constructor(
    private val log: Log,
    private val discourseService: DiscourseService,
    private val memberRepository: MemberRepository,
    private val activeDirectoryService: ActiveDirectoryService
) {
  suspend fun getMember(username: String): DMSMember {
    // Fetch member from active directory.
    val adMember = activeDirectoryService.getMember(username)

    val dbMember = memberRepository.getMemberOrInsert(username)
    dbMember.firstName = adMember.givenName
    dbMember.lastName = adMember.sn
    dbMember.displayName = adMember.displayName
    dbMember.personalEmail = adMember.mail
    dbMember.phoneNumber = getNormalizedPhoneNumber(adMember.telephoneNumber)
    dbMember.badgeNumber = adMember.employeeID
    dbMember.memberSince = calculateMemberSince(adMember.whenCreated)
    dbMember.groups =
        adMember.groups.map { group ->
          DMSGroup(group.cn, group.distinguishedName, group.objectGuid, null)
        }
    return dbMember
  }

  private fun getNormalizedPhoneNumber(telephoneNumber: String?): String? {
    if (telephoneNumber == null) {
      return null
    }
    val pnu: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    val pn: Phonenumber.PhoneNumber = pnu.parse(telephoneNumber, "US")
    val pnE164: String = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
    return pnE164
  }

  /** Calculates the memberSince date from the whenCreated string. */
  @Suppress("MagicNumber")
  private fun calculateMemberSince(whenCreated: String?): Instant? {
    if (whenCreated == null) {
      return null
    }
    log.i("Calculating memberSince from whenCreated: $whenCreated")
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
    val adMember = activeDirectoryService.getMember(username)
    // If member is not active in AD then throw an exception.
    // TODO(mandarl): Throw if member is not active in AD.
    val dbMember = memberRepository.getMemberOrInsert(username)
    var propertiesUpdated = false
    // Figure out which properties have been updated by comparing dbMember and memberFromApi.
    if (dbMember.avatarUrl != memberFromApi.avatarUrl) {
      propertiesUpdated = true
    }
    if (dbMember.discourseUsername != memberFromApi.discourseUsername) {
      propertiesUpdated = true
      if (memberFromApi.discourseUsername == null) {
        // Remove the member from the discourse group.
        discourseService.removeUserFromDmsMembersV2Group(requireNotNull(dbMember.discourseUsername))
        discourseService.addUserToDmsMembersV1Group(requireNotNull(dbMember.discourseUsername))
      } else {
        // Add the member to the discourse group.
        discourseService.removeUserFromDmsMembersV1Group(
            requireNotNull(memberFromApi.discourseUsername))
        discourseService.addUserToDmsMembersV2Group(requireNotNull(memberFromApi.discourseUsername))
      }
    }
    if (dbMember.discordUserId != memberFromApi.discordUserId) {
      propertiesUpdated = true
    }
    if (propertiesUpdated) {
      // Update the member in the database.
      memberRepository.updateMember(username, memberFromApi)
    }
  }
}
