package org.dallasmakerspace.members

import javax.inject.Inject
import kotlinx.datetime.toJavaLocalDateTime
import org.dallasmakerspace.members.db.ActivityLogDAO
import org.dallasmakerspace.members.db.ActivityLogTable
import org.dallasmakerspace.members.db.ProfileColumnAliases.actorProfileAlias
import org.dallasmakerspace.members.db.ProfileColumnAliases.subjectProfileAlias
import org.dallasmakerspace.members.db.ProfileTable
import org.dallasmakerspace.members.db.daoToActivityLogModel
import org.dallasmakerspace.members.db.suspendTransaction
import org.dallasmakerspace.models.ActivityLog
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.ActivityLogSource
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select

/** Manages the activity log associated with a member. CRUD operations using ActivityLogDAO. */
class ActivityLogRepository @Inject constructor() {

  /**
   * Fetches the activity log for a member.
   *
   * @param username The username of the member.
   * @return The activity log for the member.
   */
  suspend fun getMemberActivityLog(subjectProfileRowId: Int): List<ActivityLog> =
      suspendTransaction {
        ActivityLogTable.join(
                actorProfileAlias,
                JoinType.INNER,
                ActivityLogTable.actorProfileRowId,
                actorProfileAlias[ProfileTable.idColumn])
            .join(
                subjectProfileAlias,
                JoinType.INNER,
                ActivityLogTable.subjectProfileRowId,
                subjectProfileAlias[ProfileTable.idColumn])
            .select { ActivityLogTable.subjectProfileRowId eq subjectProfileRowId }
            .map { daoToActivityLogModel(it) }
      }

  /** Inserts a new activity log entry. */
  suspend fun insertActivityLogEntry(activityLog: ActivityLog) = suspendTransaction {
    ActivityLogDAO.new {
      source = ActivityLogSource.valueOf(activityLog.source).ordinal
      actorProfileRowId =
          ProfileTable.select { ProfileTable.username eq activityLog.actorProfileUsername }
              .first()[ProfileTable.idColumn]
              .value
      subjectProfileRowId =
          ProfileTable.select { ProfileTable.username eq activityLog.subjectProfileUsername }
              .first()[ProfileTable.idColumn]
              .value
      event = ActivityLogEvent.valueOf(activityLog.event).ordinal
      attributes = activityLog.attributes
      created = activityLog.created.toJavaLocalDateTime()
    }
  }
}
