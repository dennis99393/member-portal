package org.dallasmakerspace.members.observers

import javax.inject.Inject
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.discourse.DiscourseService
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.DMSMember

class MemberDisabledObserver
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val discourseService: DiscourseService,
    private val activityLogService: ActivityLogService
) : IMemberPropChangeObserver {
  val log = loggerFactory.create(javaClass)

  override suspend fun onMemberPropChange(
      propName: String,
      oldValue: Any?,
      newValue: Any?,
      affectedMembers: List<DMSMember>
  ) {
    log.info(
        "MemberDisabledObserver.onMemberPropChange: propName=$propName, oldValue=$oldValue, " +
            "newValue=$newValue, affectedMembers=$affectedMembers")
    if (propName == "enabled" && oldValue == true && newValue == false) {
      val usernames = affectedMembers.mapNotNull { it.discourseUsername }
      discourseService.removeUserFromDmsMembersV2Group(usernames)
      activityLogService.insertBulkActivityLogEntry(
          subjectUsernames = usernames, event = ActivityLogEvent.UNLINK_DISCOURSE)
    }
  }
}
