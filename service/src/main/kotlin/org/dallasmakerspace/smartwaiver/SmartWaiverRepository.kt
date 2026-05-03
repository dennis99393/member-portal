package org.dallasmakerspace.smartwaiver

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import org.dallasmakerspace.core.DBMasterConnection
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@Singleton
class SmartWaiverRepository @Inject constructor() {
  suspend fun insertSmartWaiver(waiver: SmartwaiverFullWaiver) {
    val customFields = waiver.customWaiverFields.values.toList()
    newSuspendedTransaction(Dispatchers.IO, db = DBMasterConnection.db) {
      SmartWaiverTable.insert {
        it[firstName] = waiver.firstName.orEmpty()
        it[lastName] = waiver.lastName.orEmpty()
        it[email] = waiver.email
        it[birthDate] = waiver.dob.orEmpty()
        it[dateCompleted] = waiver.createdOn.orEmpty()
        it[waiverId] = waiver.waiverId
        it[waiverStatus] = "Completed At Kiosk"
        it[documentTitle] = waiver.title
        it[checkinCount] = 1
        it[referralType] = customFields.getOrNull(0)?.value
        it[referralSource] = customFields.getOrNull(1)?.value
        it[requestedWaiverEmail] = 1
        it[guardianFirstName] = ""
        it[guardianLastName] = ""
      }
    }
  }
}
