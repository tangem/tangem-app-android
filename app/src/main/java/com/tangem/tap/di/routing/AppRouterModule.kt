package com.tangem.tap.di.routing

import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.RoutingFeatureToggle
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
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
    fun provideAppRouter(
        config: AppRouterConfig,
        dispatchers: CoroutineDispatcherProvider,
        analyticsExceptionHandler: AnalyticsExceptionHandler,
    ): AppRouter = ProxyAppRouter(
        config = config,
        dispatchers = dispatchers,
        analyticsExceptionHandler = analyticsExceptionHandler,
    )

    @Provides
    @Singleton
    fun provideAppRouterConfigurator(): AppRouterConfig = MutableAppRouterConfig()

    @Provides
    @Singleton
    fun provideRoutingFeatureToggle(featureTogglesManager: FeatureTogglesManager): RoutingFeatureToggle {
        return RoutingFeatureToggle(featureTogglesManager)
    }
}