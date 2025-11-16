package org.dallasmakerspace.server.models

import java.time.Instant
import kotlin.reflect.full.memberProperties
import kotlinx.datetime.LocalDate

@Suppress("LongParameterList")
class DMSMember(
    val username: String,
    var firstName: String? = null,
    var lastName: String? = null,
    var displayName: String? = null,
    val avatarUrl: String? = null,
    var personalEmail: String? = null,
    var phoneNumber: String? = null,
    var badgeNumber: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var discordUserId: String? = null,
    var memberSince: Instant? = null,
    var enabled: Boolean = false,
    var groups: List<DMSGroup> = emptyList(),
    var accountInfo: AccountInfo? = null,
) {
  override fun toString(): String {
    // Use reflection to generate a string containing all the properties of the class
    return this::class.memberProperties.joinToString(", ") { "${it.name}: ${it.getter.call(this)}" }
  }

  companion object {
    fun fromMap(data: Map<String, Any?>): DMSMember {
      val groups =
          (data["groups"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            if (it["data"] is Map<*, *>) {
              val dataGroup = it["data"] as Map<String, Any?>
              DMSGroup.fromMap(dataGroup)
            } else {
              DMSGroup.fromMap(it as Map<String, Any?>)
            }
          } ?: emptyList()
      val accountInfo = data["accountInfo"]?.let { AccountInfo.fromMap(it as Map<String, Any?>) }
      return DMSMember(
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

    fun toMap(member: DMSMember): Map<String, Any?> {
      return mapOf(
          "username" to member.username,
          "avatarUrl" to member.avatarUrl,
          "discourseUsername" to member.discourseUsername,
          "discourseAvatarUrl" to member.discourseAvatarUrl,
          "discordUserId" to member.discordUserId,
      )
    }
  }
}

/** Data class to hold various account related properties. */
data class AccountInfo(
    var wasActivePast90Days: Boolean? = null,
    var lastInactiveDate: LocalDate? = null,
    var isPrimaryAccount: Boolean = false,
    var addonAccounts: List<Account> = emptyList(),
    var primaryAccount: Account? = null,
) {
  companion object {
    fun fromMap(map: Map<String, Any?>): AccountInfo? {
      return if (map.isEmpty()) {
        null
      } else {
        AccountInfo(
            wasActivePast90Days = map["wasActivePast90Days"] as? Boolean,
            lastInactiveDate = (map["lastInactiveDate"] as? String)?.let { LocalDate.parse(it) },
            isPrimaryAccount = map["isPrimaryAccount"] as Boolean,
            addonAccounts =
                (map["addonAccounts"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
                  Account.fromMap(it)
                } ?: emptyList(),
            primaryAccount =
                (map["primaryAccount"] as? Map<String, Any?>)?.let { Account.fromMap(it) })
      }
    }
  }
}

/** Data class to represent an account - may be primary or addon. */
data class Account(
    val username: String,
    val whmcsId: Int,
    val isActive: Boolean,
) {
  companion object {
    fun fromMap(map: Map<String, Any?>): Account {
      return Account(
          username = map["username"] as String,
          whmcsId = map["whmcsId"] as Int,
          isActive = map["isActive"] as Boolean)
    }
  }
}
