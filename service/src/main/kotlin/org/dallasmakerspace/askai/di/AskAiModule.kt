package org.dallasmakerspace.askai.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton
import org.dallasmakerspace.askai.AskAiConfig
import org.dallasmakerspace.askai.confluence.ConfluenceApiClient
import org.dallasmakerspace.askai.confluence.ConfluenceApiClientMock
import org.dallasmakerspace.askai.confluence.IConfluenceApiClient
import org.dallasmakerspace.askai.openrouter.IOpenRouterClient
import org.dallasmakerspace.askai.openrouter.OpenRouterClient
import org.dallasmakerspace.askai.openrouter.OpenRouterClientMock
import org.dallasmakerspace.askai.search.ConfluenceSearchSource
import org.dallasmakerspace.askai.search.DiscourseSearchSource
import org.dallasmakerspace.askai.search.SearchSource
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

/**
 * Dagger module for the Ask AI feature. Provides all dependencies for the AI-powered Q&A system.
 */
@Module
class AskAiModule {

  @Provides
  fun provideOpenRouterClient(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): IOpenRouterClient =
      if (useMockServices(appConfig)) {
        OpenRouterClientMock(loggerFactory)
      } else {
        OpenRouterClient(appConfig, loggerFactory)
      }

  @Provides
  fun provideConfluenceApiClient(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): IConfluenceApiClient =
      if (useMockServices(appConfig)) {
        ConfluenceApiClientMock(loggerFactory)
      } else {
        ConfluenceApiClient(appConfig, loggerFactory)
      }

  /**
   * Provides AskAI service configuration. Uses default values but could be extended to read from
   * AppConfig.
   */
  @Provides
  @Singleton
  fun provideAskAiConfig(): AskAiConfig {
    return AskAiConfig()
  }

  /**
   * Provides the set of search sources. To add a new search source:
   * 1. Create a class implementing SearchSource
   * 2. Add it as a parameter here
   * 3. Add it to the returned set
   */
  @Provides
  @ElementsIntoSet
  fun provideSearchSources(
      discourseSearchSource: DiscourseSearchSource,
      confluenceSearchSource: ConfluenceSearchSource
  ): Set<SearchSource> {
    return setOf(discourseSearchSource, confluenceSearchSource)
  }

  private fun useMockServices(appConfig: AppConfig) =
      appConfig.requireBooleanProperty("ktor.development") &&
          appConfig.requireBooleanProperty("app.use-mock-services")
}
