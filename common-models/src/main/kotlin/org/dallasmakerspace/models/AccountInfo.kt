package org.dallasmakerspace.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** Data class to hold various account related properties. */
@Serializable
data class AccountInfo(
    var isPrimaryAccount: Boolean = false,
    var wasActivePast90Days: Boolean? = null,
    var lastInactiveDate: LocalDate? = null,
    var addonAccounts: List<Account> = emptyList(),
    var primaryAccount: Account? = null,
    var regDate: LocalDate? = null,
)

/** Data class to represent an account - may be primary or addon. */
@Serializable
data class Account(
    val username: String,
    val whmcsId: Int,
    val isActive: Boolean,
)
