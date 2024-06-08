package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.Log

@Module
class AppModule {

  @Provides fun provideAppConfig() = AppConfig()

  @Provides fun provideLog() = Log()
}
