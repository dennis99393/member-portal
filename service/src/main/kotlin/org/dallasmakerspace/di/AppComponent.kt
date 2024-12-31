package org.dallasmakerspace.di

import dagger.Component
import io.ktor.server.application.*
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.DBMasterConnection
import org.dallasmakerspace.core.DBMemberPortalConnection
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.cron.MemberRefreshCronJob
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.members.MemberService

@Singleton
@Component(
    modules =
        [
            AppModule::class,
            ActiveDirectoryModule::class,
            DiscourseModule::class,
            MembersModule::class])
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig

  fun getMemberService(): MemberService

  fun getActivityLogService(): ActivityLogService

  fun getDBConnection(): DBMemberPortalConnection

  fun getDBMasterConnection(): DBMasterConnection

  fun getLoggerFactory(): LoggerFactory

  fun getMemberRefreshCronJob(): MemberRefreshCronJob
}
