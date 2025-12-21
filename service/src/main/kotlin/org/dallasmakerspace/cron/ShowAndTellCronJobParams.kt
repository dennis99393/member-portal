package org.dallasmakerspace.cron

data class ShowAndTellCronJobParams(val isRunningInShadowMode: Boolean = true) : CronJobParams {
  override fun toString(): String {
    return "[isRunningInShadowMode=$isRunningInShadowMode]"
  }
}
