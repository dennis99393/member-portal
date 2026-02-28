package org.dallasmakerspace.di

import dagger.Component
import io.ktor.server.application.*
import javax.inject.Singleton
import org.dallasmakerspace.askai.AskAiService
import org.dallasmakerspace.discourse.FeaturedProjectsService
import org.dallasmakerspace.askai.di.AskAiModule
import org.dallasmakerspace.calendar.CalendarService
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.DBMasterConnection
import org.dallasmakerspace.core.DBMemberPortalConnection
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.cron.DoorSwipesCronJob
import org.dallasmakerspace.cron.MemberRefreshCronJob
import org.dallasmakerspace.cron.ShowAndTellCronJob
import org.dallasmakerspace.dataviz.DataVizRouter
import org.dallasmakerspace.dataviz.di.DataVizModule
import org.dallasmakerspace.doorcontroller.DoorControllerService
import org.dallasmakerspace.members.ActivityLogService
import org.dallasmakerspace.members.GroupService
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.shortlinks.ShortLinksService
import org.dallasmakerspace.smartwaiver.SmartwaiverModule
import org.dallasmakerspace.webhook.WebhookRouter
import org.dallasmakerspace.webhook.di.WebhookModule

@Singleton
@Component(
    modules =
        [
            AppModule::class,
            ActiveDirectoryModule::class,
            AskAiModule::class,
            CalendarModule::class,
            DiscourseModule::class,
            DoorControllerModule::class,
            MembersModule::class,
            DataVizModule::class,
            ShortLinksModule::class,
            SmartwaiverModule::class,
            WebhookModule::class])
@Suppress("TooManyFunctions")
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig

  fun getMemberService(): MemberService

  fun getGroupService(): GroupService

  fun getActivityLogService(): ActivityLogService

  fun getDBConnection(): DBMemberPortalConnection

  fun getDBMasterConnection(): DBMasterConnection

  fun getLoggerFactory(): LoggerFactory

  fun getMemberRefreshCronJob(): MemberRefreshCronJob

  fun getShowAndTellCronJob(): ShowAndTellCronJob

  fun getDataVizRouter(): DataVizRouter

  fun getWebhookRouter(): WebhookRouter

  fun getDoorControllerService(): DoorControllerService

  fun getDoorSwipesCronJob(): DoorSwipesCronJob

  fun getCalendarService(): CalendarService

  fun getShortLinksService(): ShortLinksService

  fun getAskAiService(): AskAiService

  fun getFeaturedProjectsService(): FeaturedProjectsService
}
