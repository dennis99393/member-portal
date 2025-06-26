package org.dallasmakerspace.webhook.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import org.dallasmakerspace.webhook.WebhookHandler
import org.dallasmakerspace.webhook.handlers.GroupHistoryWebhook

@Module
class WebhookModule {
  @Provides
  @ElementsIntoSet
  fun provideWebhookHandlers(
      groupHistoryWebhook: GroupHistoryWebhook,
  ): Set<WebhookHandler> {
    return setOf(groupHistoryWebhook)
  }
}
