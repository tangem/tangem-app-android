package com.tangem.tap.di.routing

import com.tangem.common.routing.AppRouter
import com.tangem.tap.routing.DefaultNavigationFailureHandler
import com.tangem.tap.routing.ProxyAppRouter
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.configurator.MutableAppRouterConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppRouterModule {

    @Provides
    @Singleton
    fun provideAppRouter(
        config: AppRouterConfig,
        navigationFailureHandler: DefaultNavigationFailureHandler,
    ): AppRouter = ProxyAppRouter(config = config, navigationFailureHandler = navigationFailureHandler)

    @Provides
    @Singleton
    fun provideAppRouterConfigurator(): AppRouterConfig = MutableAppRouterConfig()
}