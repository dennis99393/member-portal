package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
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
  fun providesIndexHandler(userInfoProvider: UserInfoProvider): IRouteHandler =
      IndexHandler(userInfoProvider)

  @IntoMap @Provides @StringKey("/login") fun providesLoginHandler(): IRouteHandler = LoginHandler()

  @IntoMap
  @Provides
  @StringKey("/profile/@{preferred_username}")
  fun providesProfileHandler(userInfoProvider: UserInfoProvider): IRouteHandler =
      ProfileHandler(userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/oidc-callback")
  fun providesOidcCallbackHandler(): IRouteHandler = OidcCallbackHandler()
}
