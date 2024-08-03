package org.dallasmakerspace.cron

interface ICronJob {
  suspend fun run(): String
}
