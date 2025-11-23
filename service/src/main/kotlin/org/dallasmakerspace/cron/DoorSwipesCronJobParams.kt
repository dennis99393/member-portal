package org.dallasmakerspace.cron

data class DoorSwipesCronJobParams(
    val minutes: Int = DEFAULT_MINUTES
) : CronJobParams {
  override fun toString(): String {
    return "[minutes=$minutes]"
  }

  companion object {
    const val DEFAULT_MINUTES = 60
  }
}
