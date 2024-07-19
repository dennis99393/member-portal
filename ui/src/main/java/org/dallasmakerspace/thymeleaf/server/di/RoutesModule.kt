package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler
import org.dallasmakerspace.thymeleaf.server.routes.IndexHandler
import org.dallasmakerspace.thymeleaf.server.routes.LoginHandler
import org.dallasmakerspace.thymeleaf.server.routes.OidcCallbackHandler
import org.dallasmakerspace.thymeleaf.server.routes.ProfileHandler

@Module
class RoutesModule {

  @IntoMap
  @Provides
  @StringKey("/")
  fun providesIndexHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider
  ): IRouteHandler = IndexHandler(loggerFactory, userInfoProvider)

  @IntoMap @Provides @StringKey("/login") fun providesLoginHandler(): IRouteHandler = LoginHandler()

  @IntoMap
  @Provides
  @StringKey("/profile/@{preferred_username}")
  fun providesProfileHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = ProfileHandler(loggerFactory, memberService, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/oidc-callback")
  fun providesOidcCallbackHandler(): IRouteHandler = OidcCallbackHandler()
}
