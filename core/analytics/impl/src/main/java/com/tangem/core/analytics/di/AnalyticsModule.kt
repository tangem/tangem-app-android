package com.tangem.core.analytics.di

import com.tangem.core.analytics.DefaultAnalyticsEventHandler
import com.tangem.core.analytics.onetime.DefaultOneTimeEventStore
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.OneTimeEventStore
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.filter.AppsFlyerEventFilter
import com.tangem.core.analytics.filter.OneTimeEventFilter
import com.tangem.core.analytics.paramsinterceptor.SendTransactionSignerInfoInterceptor
import com.tangem.core.analytics.utils.TrackingContextProxy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsModule {

    @Binds
    fun bindAnalyticsEventHandler(impl: DefaultAnalyticsEventHandler): AnalyticsEventHandler

    @Binds
    fun bindAnalyticsErrorHandler(impl: DefaultAnalyticsEventHandler): AnalyticsErrorHandler

    @Binds
    fun bindAnalyticsExceptionHandler(impl: DefaultAnalyticsEventHandler): AnalyticsExceptionHandler

    @Binds
    fun bindAnalyticsUserIdHandler(impl: DefaultAnalyticsEventHandler): AnalyticsUserIdHandler

    @Binds
    fun bindParamsInterceptorHolder(impl: DefaultAnalyticsEventHandler): ParamsInterceptorHolder

    @Binds
    @Singleton
    fun bindOneTimeEventStore(impl: DefaultOneTimeEventStore): OneTimeEventStore

    @Multibinds
    fun analyticsHandlers(): Set<AnalyticsHandler>

    @Multibinds
    fun trackingContextObservers(): Set<TrackingContextProxy.Observer>

    @Binds
    @IntoSet
    fun bindSendTransactionSignerInfoInterceptor(impl: SendTransactionSignerInfoInterceptor): ParamsInterceptor

    companion object {

        /**
         * Order matters: an event that is both one-time and AppsFlyer-included must reach [OneTimeEventFilter],
         * otherwise it is sent on every occurrence.
         */
        @Provides
        fun provideAnalyticsEventFilters(oneTimeEventFilter: OneTimeEventFilter): List<AnalyticsEventFilter> {
            return listOf(oneTimeEventFilter, AppsFlyerEventFilter())
        }
    }
}