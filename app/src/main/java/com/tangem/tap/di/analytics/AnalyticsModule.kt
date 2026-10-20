package com.tangem.tap.di.analytics

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.tap.common.analytics.ABTestsTrackingContextObserver
import com.tangem.tap.common.analytics.DefaultTrackingContextProxy
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAppInstanceIdProvider
import com.tangem.tap.common.analytics.handlers.opentelemetry.DefaultOtelFeatureToggles
import com.tangem.tap.common.analytics.handlers.opentelemetry.OtelFeatureToggles
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsContextProxy(
        paramsInterceptorHolder: ParamsInterceptorHolder,
        userIdHandler: AnalyticsUserIdHandler,
        observers: Set<@JvmSuppressWildcards TrackingContextProxy.Observer>,
        cardContextInterceptorFactory: CardContextInterceptor.Factory,
    ): TrackingContextProxy = DefaultTrackingContextProxy(
        paramsInterceptorHolder = paramsInterceptorHolder,
        userIdHandler = userIdHandler,
        observers = observers,
        cardContextInterceptorFactory = cardContextInterceptorFactory,
    )

    @Provides
    @IntoSet
    fun provideABTestsTrackingContextObserver(abTestsManager: ABTestsManager): TrackingContextProxy.Observer =
        ABTestsTrackingContextObserver(abTestsManager)

    @Provides
    @Singleton
    fun provideAppInstanceIdProvider(): AppInstanceIdProvider = FirebaseAppInstanceIdProvider()

    @Provides
    @Singleton
    fun provideOtelFeatureToggles(featureTogglesManager: FeatureTogglesManager): OtelFeatureToggles =
        DefaultOtelFeatureToggles(featureTogglesManager)
}