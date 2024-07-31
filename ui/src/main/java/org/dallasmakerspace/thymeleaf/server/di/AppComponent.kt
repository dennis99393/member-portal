package org.dallasmakerspace.thymeleaf.server.di

import dagger.Component
import io.ktor.server.application.*
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.common.AppConfig
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory

@Singleton
@Component(modules = [DiscourseModule::class, RoutesModule::class, ServerModule::class])
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig

  fun getLoggerFactory(): LoggerFactory
}
