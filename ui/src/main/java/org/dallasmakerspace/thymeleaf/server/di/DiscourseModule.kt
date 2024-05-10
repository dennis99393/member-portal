package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseCallbackHandler
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseSSOProvider
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseUtil
import org.dallasmakerspace.thymeleaf.server.discourse.LinkDiscourseHandler
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler

@Module
class DiscourseModule {

  @Provides fun providesDiscourseUtil(): DiscourseUtil = DiscourseUtil()

  @Provides
  fun providesDiscourseSSOProvider(discourseUtil: DiscourseUtil): DiscourseSSOProvider =
      DiscourseSSOProvider(discourseUtil)

  @IntoMap
  @Provides
  @StringKey("/link-discourse")
  fun providesLinkDiscourseHandler(
      userInfoProvider: UserInfoProvider,
      discourseSSOProvider: DiscourseSSOProvider
  ): IRouteHandler = LinkDiscourseHandler(userInfoProvider, discourseSSOProvider)

  @IntoMap
  @Provides
  @StringKey("/discourse-callback")
  fun providesDiscourseHandler(discourseUtil: DiscourseUtil): IRouteHandler =
      DiscourseCallbackHandler(discourseUtil)
}
