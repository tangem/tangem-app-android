package com.tangem.tap.di.routing

import com.tangem.common.routing.AppRouter
import com.tangem.tap.routing.ProxyAppRouter
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.tap.routing.configurator.MutableAppRouterConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun provideAppRouter(config: AppRouterConfig, dispatchers: CoroutineDispatcherProvider): AppRouter =
        ProxyAppRouter(config, dispatchers)

    @Provides
    @Singleton
    fun provideAppRouterConfigurator(): AppRouterConfig = MutableAppRouterConfig()
}