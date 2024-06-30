package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.discourse.DiscourseService
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember

class MemberService
@Inject
constructor(
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
    dbMember.groups =
        adMember.groups.map { group ->
          DMSGroup(group.cn, group.distinguishedName, group.objectGuid, null)
        }
    return dbMember
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
      } else {
        // Add the member to the discourse group.
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
