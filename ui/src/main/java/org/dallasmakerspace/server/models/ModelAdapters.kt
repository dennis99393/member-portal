package org.dallasmakerspace.server.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.dallasmakerspace.models.Account
import org.dallasmakerspace.models.AccountInfo
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember

/** Adapter functions to convert API response maps to common-models domain objects */

fun dmsMemberFromMap(data: Map<String, Any?>): DMSMember {
  val groups =
      (data["groups"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
        if (it["data"] is Map<*, *>) {
          val dataGroup = it["data"] as Map<String, Any?>
          dmsGroupFromMap(dataGroup)
        } else {
          dmsGroupFromMap(it as Map<String, Any?>)
        }
      } ?: emptyList()
  val accountInfo = data["accountInfo"]?.let { accountInfoFromMap(it as Map<String, Any?>) }
  return DMSMember(
      id = (data["id"] as? Int) ?: 0,
      username = data["username"] as String,
      firstName = data["firstName"] as String?,
      lastName = data["lastName"] as String?,
      displayName = data["displayName"] as String?,
      avatarUrl = data["avatarUrl"] as String?,
      personalEmail = data["personalEmail"] as String?,
      phoneNumber = data["phoneNumber"] as String?,
      badgeNumber = data["badgeNumber"] as String?,
      enabled = data["enabled"] as Boolean,
      discourseUsername = data["discourseUsername"] as String?,
      discourseAvatarUrl = data["discourseAvatarUrl"] as String?,
      discordUserId = data["discordUserId"] as String?,
      memberSince = (data["memberSince"] as? String)?.let { Instant.parse(it) },
      groups = groups,
      accountInfo = accountInfo)
}

fun dmsGroupFromMap(data: Map<String, Any?>): DMSGroup {
  val members =
      (data["members"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
        dmsMemberFromMap(it)
      } ?: emptyList()
  val administrators =
      (data["administrators"] as List<*>?)?.filterIsInstance<String>() ?: emptyList()
  val history =
      (data["history"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
        groupHistoryEventFromMap(it)
      } ?: emptyList()
  return DMSGroup(
      name = data["name"] as String,
      description = data["description"] as String?,
      distinguishedName = data["distinguishedName"] as String,
      objectGuid = data["objectGuid"] as String?,
      membersListIncomplete = data["membersListIncomplete"] as Boolean,
      members = members,
      administrators = administrators,
      history = history,
  )
}

fun groupHistoryEventFromMap(data: Map<String, Any?>): org.dallasmakerspace.models.GroupHistory {
  val timestampStr = data["eventTimestamp"] as String
  // Handle timestamps that may not have seconds (e.g., "2025-10-08T01:40" -> "2025-10-08T01:40:00")
  val normalizedTimestamp =
      if (timestampStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$"))) {
        "${timestampStr}:00"
      } else {
        timestampStr
      }

  return org.dallasmakerspace.models.GroupHistory(
      actorUsername = data["actorUsername"] as String,
      memberUsername = data["memberUsername"] as String,
      eventTimestamp = kotlinx.datetime.LocalDateTime.parse(normalizedTimestamp),
  )
}

fun accountInfoFromMap(map: Map<String, Any?>): AccountInfo? {
  return if (map.isEmpty()) {
    null
  } else {
    AccountInfo(
        wasActivePast90Days = map["wasActivePast90Days"] as? Boolean,
        lastInactiveDate = (map["lastInactiveDate"] as? String)?.let { LocalDate.parse(it) },
        isPrimaryAccount = map["isPrimaryAccount"] as Boolean,
        addonAccounts =
            (map["addonAccounts"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
              accountFromMap(it)
            } ?: emptyList(),
        primaryAccount = (map["primaryAccount"] as? Map<String, Any?>)?.let { accountFromMap(it) },
        regDate = (map["regDate"] as? String)?.let { LocalDate.parse(it) })
  }
}

fun accountFromMap(map: Map<String, Any?>): Account {
  return Account(
      username = map["username"] as String,
      whmcsId = map["whmcsId"] as Int,
      isActive = map["isActive"] as Boolean)
}

/** Convert a DMSMember to a map for API requests (partial fields only) */
fun dmsMemberToMap(member: DMSMember): Map<String, Any?> {
  return mapOf(
      "username" to member.username,
      "avatarUrl" to member.avatarUrl,
      "discourseUsername" to member.discourseUsername,
      "discourseAvatarUrl" to member.discourseAvatarUrl,
      "discordUserId" to member.discordUserId,
  )
}
