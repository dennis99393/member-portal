package org.dallasmakerspace.server.di

import dagger.Component
import io.ktor.server.application.*
import javax.inject.Singleton
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory

@Singleton
@Component(modules = [DiscourseModule::class, RoutesModule::class, ServerModule::class])
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig

  fun getLoggerFactory(): LoggerFactory
}
