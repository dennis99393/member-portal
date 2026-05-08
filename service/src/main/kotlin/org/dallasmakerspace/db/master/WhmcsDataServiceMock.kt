package org.dallasmakerspace.db.master

import java.time.LocalDate
import javax.inject.Inject

class WhmcsDataServiceMock @Inject constructor() : IWhmcsDataService {

  private fun mockStatus() =
      AccountStatus(
          wasActiveInRange = true,
          lastInactiveDate = null,
          regDate = LocalDate.of(2021, 1, 1),
          daysInCurrentStatus = null,
          timeline = emptyList(),
      )

  override suspend fun getAccountInfoMap(whmcsIdList: List<Int>): Map<Int, AccountStatus> =
      whmcsIdList.associateWith { mockStatus() }

  override suspend fun getAccountInfoMap(
      whmcsIdList: List<Int>,
      startDate: LocalDate
  ): Map<Int, AccountStatus> = whmcsIdList.associateWith { mockStatus() }

  override suspend fun getAccountRegdate(whmcsId: Int): LocalDate? = LocalDate.of(2021, 1, 1)
}
