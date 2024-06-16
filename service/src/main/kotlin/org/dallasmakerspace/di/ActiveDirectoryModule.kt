package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.activedirectory.ActiveDirectoryClient
import org.dallasmakerspace.activedirectory.ActiveDirectoryClientMock
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.activedirectory.ActiveDirectoryServiceMock
import org.dallasmakerspace.activedirectory.IActiveDirectoryClient
import org.dallasmakerspace.activedirectory.IActiveDirectoryService
import org.dallasmakerspace.core.AppConfig

@Module
class ActiveDirectoryModule {

  @Provides
  fun providesActiveDirectoryClient(appConfig: AppConfig): IActiveDirectoryClient =
      if (appConfig.requireProperty("ktor.development").getString() == "true") {
        ActiveDirectoryClientMock()
        // ActiveDirectoryClient(appConfig)
      } else {
        ActiveDirectoryClient(appConfig)
      }

  @Provides
  fun provideActiveDirectoryService(
      appConfig: AppConfig,
      activeDirectoryClient: IActiveDirectoryClient
  ): IActiveDirectoryService =
      if (appConfig.requireProperty("ktor.development").getString() == "true") {
        ActiveDirectoryServiceMock()
        // ActiveDirectoryService(appConfig, activeDirectoryClient)
      } else {
        ActiveDirectoryService(activeDirectoryClient)
      }
}
