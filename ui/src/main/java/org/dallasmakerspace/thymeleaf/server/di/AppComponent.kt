package org.dallasmakerspace.thymeleaf.server.di

import dagger.Component
import io.ktor.server.application.*
import org.dallasmakerspace.thymeleaf.server.common.AppConfig
import javax.inject.Singleton

@Singleton
@Component(modules = [DiscourseModule::class, RoutesModule::class, ServerModule::class])
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig
}
