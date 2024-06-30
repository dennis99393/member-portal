package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.Log
import org.dallasmakerspace.discourse.DiscourseApiClient
import org.dallasmakerspace.discourse.DiscourseApiClientMock
import org.dallasmakerspace.discourse.IDiscourseApiClient

@Module
class DiscourseModule {

  @Provides
  fun provideDiscourseApiClient(appConfig: AppConfig, log: Log): IDiscourseApiClient =
      if (appConfig.requireBooleanProperty("ktor.development")) {
        DiscourseApiClientMock(appConfig, log)
        // DiscourseApiClient(appConfig, log)
      } else {
        DiscourseApiClient(appConfig, log)
      }
}
