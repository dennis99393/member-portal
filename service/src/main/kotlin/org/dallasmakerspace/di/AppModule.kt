package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.core.AppConfig

@Module
class AppModule {

  @Provides fun provideAppConfig() = AppConfig()
}
