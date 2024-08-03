package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.members.db.ActivityLogDAO
import org.dallasmakerspace.members.db.ActivityLogTable
import org.dallasmakerspace.members.db.ProfileColumnAliases.actorProfileAlias
import org.dallasmakerspace.members.db.ProfileColumnAliases.subjectProfileAlias
import org.dallasmakerspace.members.db.ProfileTable
import org.dallasmakerspace.members.db.daoToActivityLogModel
import org.dallasmakerspace.members.db.suspendTransaction
import org.dallasmakerspace.models.ActivityLog
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select

/** Manages the activity log associated with a member. CRUD operations using ActivityLogDAO. */
class ActivityLogRepository @Inject constructor() {

  /**
   * Fetches the activity log for a member.
   *
   * @param subjectProfileRowId The member.
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
    ActivityLogDAO.new(null) {
      source = activityLog.source.value
      actorProfileRowId =
          ProfileTable.select { ProfileTable.username eq activityLog.actorProfileUsername }
              .first()[ProfileTable.idColumn]
              .value
      subjectProfileRowId =
          ProfileTable.select { ProfileTable.username eq activityLog.subjectProfileUsername }
              .first()[ProfileTable.idColumn]
              .value
      event = activityLog.event.value
      attributes = activityLog.attributes
    }
  }

  suspend fun insertBulkActivityLogEntry(list: List<ActivityLog>) {
    // TODO: Implement batch insert
    list.forEach { insertActivityLogEntry(it) }
  }
}
