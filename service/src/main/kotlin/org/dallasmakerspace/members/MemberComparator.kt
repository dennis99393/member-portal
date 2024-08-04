package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.activedirectory.ADUser
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.members.observers.IMemberPropChangeObserver
import org.dallasmakerspace.models.DMSMember

class MemberComparator
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberRepository: MemberRepository,
    private val memberPropChangeObservers: Set<@JvmSuppressWildcards IMemberPropChangeObserver>
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun process(memberList: List<Pair<DMSMember, ADUser?>>) {
    log.info(
        "MemberComparator.process: memberList=${memberList.joinToString(separator = ",") { it.first.username }}")
    // List of members with enabled prop changed
    memberList.forEach { (dbMember, adUser) ->
      log.debug(
          "Member ${dbMember.username} enabled changed: ${dbMember.enabled} -> ${adUser?.enabled}")
    }
    val enabledChangedMembers =
        memberList.filter { (dbMember, adUser) -> dbMember.enabled && adUser?.enabled == false }

    log.debug(
        "Enabled changed members: ${enabledChangedMembers.size}: " +
            enabledChangedMembers.joinToString(separator = ",") { it.first.username })

    // Notify observers of any changes
    memberPropChangeObservers.forEach { observer ->
      val result =
          observer.onMemberPropChange(
              "enabled", true, false, enabledChangedMembers.map { it.first })
      if (result) {
        val updatedMembers = enabledChangedMembers.map { it.first }
        // Set .enabled to false for all members in the list
        updatedMembers.forEach { it.enabled = false }
        log.debug(
            "Updating members: ${updatedMembers.joinToString(separator = ",") { it.username }}")
        memberRepository.updateMembers(updatedMembers)
      }
    }
  }
}
