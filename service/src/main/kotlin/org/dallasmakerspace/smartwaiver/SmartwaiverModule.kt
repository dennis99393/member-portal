package org.dallasmakerspace.smartwaiver

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

@Module
class SmartwaiverModule {

  @Provides
  fun provideSmartwaiverApiClient(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): ISmartwaiverApiClient =
      if (useMockServices(appConfig)) {
        SmartwaiverApiClientMock(loggerFactory)
      } else {
        SmartwaiverApiClient(appConfig, loggerFactory)
      }

  private fun useMockServices(appConfig: AppConfig) =
      appConfig.requireBooleanProperty("ktor.development") &&
          appConfig.requireBooleanProperty("app.use-mock-services")
}
