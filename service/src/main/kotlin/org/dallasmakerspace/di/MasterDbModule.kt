package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.Time
import org.dallasmakerspace.db.master.IMakerManagerDataService
import org.dallasmakerspace.db.master.IWhmcsDataService
import org.dallasmakerspace.db.master.MakerManagerDataRepository
import org.dallasmakerspace.db.master.MakerManagerDataService
import org.dallasmakerspace.db.master.MakerManagerDataServiceMock
import org.dallasmakerspace.db.master.WhmcsDataRepository
import org.dallasmakerspace.db.master.WhmcsDataService
import org.dallasmakerspace.db.master.WhmcsDataServiceMock
import org.dallasmakerspace.voterregistration.VoterRegistrationManager

@Module
class MasterDbModule {

  @Provides
  @Singleton
  fun provideMakerManagerDataService(appConfig: AppConfig): IMakerManagerDataService =
      if (useMockServices(appConfig)) {
        MakerManagerDataServiceMock()
      } else {
        MakerManagerDataService(MakerManagerDataRepository())
      }

  @Provides
  @Singleton
  fun provideWhmcsDataService(
      appConfig: AppConfig,
      time: Time,
      voterRegistrationManager: VoterRegistrationManager,
  ): IWhmcsDataService =
      if (useMockServices(appConfig)) {
        WhmcsDataServiceMock()
      } else {
        WhmcsDataService(time, WhmcsDataRepository(), voterRegistrationManager)
      }

  private fun useMockServices(appConfig: AppConfig) =
      appConfig.requireBooleanProperty("ktor.development") &&
          appConfig.requireBooleanProperty("app.use-mock-services")
}
