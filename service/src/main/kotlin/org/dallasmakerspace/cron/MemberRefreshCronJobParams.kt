package org.dallasmakerspace.cron

data class MemberRefreshCronJobParams(
    val isRunningInShadowMode: Boolean = true,
    val refreshAvatars: Boolean = true
) : CronJobParams {
  override fun toString(): String {
    return "[isRunningInShadowMode=$isRunningInShadowMode, refreshAvatars=$refreshAvatars]"
  }
}
