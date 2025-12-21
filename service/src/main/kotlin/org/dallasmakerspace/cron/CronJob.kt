package org.dallasmakerspace.cron

import kotlin.reflect.KClass
import org.dallasmakerspace.core.LoggerFactory

/** Marker interface for a cron job. */
abstract class CronJob<T : CronJobParams>(klass: KClass<out T>, loggerFactory: LoggerFactory) {
  val log = loggerFactory.createInMemoryLogger(klass.java)

  /** Run the cron job. Accepts argument of Type ICronJobParams */
  abstract suspend fun run(params: T): String
}
