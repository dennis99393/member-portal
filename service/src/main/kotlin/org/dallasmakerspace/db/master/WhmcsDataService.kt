package org.dallasmakerspace.db.master

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.Time
import org.dallasmakerspace.voterregistration.UserGracePeriod
import org.dallasmakerspace.voterregistration.VoterRegistrationManager

@Singleton
class WhmcsDataService
@Inject
constructor(
    private val time: Time,
    private val whmcsDataRepository: WhmcsDataRepository,
    private val voterRegistrationManager: VoterRegistrationManager
) {
  /** Get account info for a list of WHMCS user IDs. */
  suspend fun getAccountInfoMap(whmcsIdList: List<Int>): Map<Int, AccountStatus> {
    val startDate: LocalDate =
        time.getToday().minusDays(VoterRegistrationManager.MEMBER_IN_GOOD_STANDING_DAYS)
    val accountProductsInfoMap =
        whmcsDataRepository.getAccountProductInfoMap(whmcsIdList, startDate)
    val result = mutableMapOf<Int, AccountStatus>()
    whmcsIdList.forEach { whmcsId ->
      val products = accountProductsInfoMap[whmcsId] ?: emptyList()
      result[whmcsId] =
          checkActiveInRange(products, startDate, voterRegistrationManager.getUserGracePeriods())
    }
    return result
  }

  /**
   * Check if the user has at least one active product from the startDate to now. Product is
   * considered active between regdate and termination_date, if it is currently active then
   * domainStatus is DomainStatus.Active and termination_date is null. We cater the edge case where
   * the user switched from one active product to another on the same day.
   *
   * @param whmcsId WHMCS user ID
   * @param products List of products for the user
   * @param startDate Start date for the check
   * @return True if the user has at least active product between start date and now, false
   *   otherwise
   */
  private fun checkActiveInRange(
      products: List<WhmcsProductInfo>,
      startDate: LocalDate,
      userGracePeriods: List<UserGracePeriod>
  ): AccountStatus {
    val today = time.getToday()
    val userProducts = products.sortedBy { it.regDate }
    var lastInactiveDate: LocalDate? = null
    var hasAnyInactiveDate = false

    // For each date between start date and now, check if the user has an active product
    var currentDate = startDate
    while (!currentDate.isAfter(today)) {
      val dateIsCovered =
          userProducts.any { product ->
            // Product covers a date if:
            // 1. Registration date is on or before the date we're checking
            // 2. And either:
            //    - Product is currently active (Active + null termination)
            //    - Or termination date is after the date we're checking
            val isCurrentlyActive =
                product.domainStatus == WhmcsDomainStatus.Active && product.terminationDate == null
            val isTerminatedAfterDate =
                product.terminationDate?.isAfter(currentDate) == true ||
                    product.terminationDate?.isEqual(currentDate) == true

            !product.regDate.isAfter(currentDate) && (isCurrentlyActive || isTerminatedAfterDate)
          }

      val dateInGracePeriod =
          userGracePeriods.any { gracePeriod ->
            !currentDate.isBefore(gracePeriod.startDate) &&
                !currentDate.isAfter(gracePeriod.endDate)
          }

      if (!dateIsCovered && !dateInGracePeriod) {
        lastInactiveDate = currentDate
        hasAnyInactiveDate = true
      }
      currentDate = currentDate.plusDays(1)
    }

    return AccountStatus(
        wasActiveInRange = !hasAnyInactiveDate,
        lastInactiveDate = lastInactiveDate,
        regDate = userProducts.firstOrNull()?.regDate)
  }

  suspend fun getAccountRegdate(whmcsId: Int): LocalDate? {
    return whmcsDataRepository.getAccountRegdate(whmcsId)
  }
}

// Data structure to hold whether the account was active in the given range and the last inactive
// date
data class AccountStatus(
    val wasActiveInRange: Boolean,
    val lastInactiveDate: LocalDate?,
    val regDate: LocalDate?
)
