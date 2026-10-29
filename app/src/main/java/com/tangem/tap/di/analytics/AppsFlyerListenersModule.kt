package com.tangem.tap.di.analytics

import com.tangem.core.analytics.api.AppsFlyerConversionDataHandler
import com.tangem.core.analytics.api.AppsFlyerDeepLinkHandler
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerReferralParamsHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface AppsFlyerListenersModule {

    @Binds
    fun bindDeepLinkHandler(handler: AppsFlyerReferralParamsHandler): AppsFlyerDeepLinkHandler

    @Binds
    @IntoSet
    fun bindReferralParamsHandler(handler: AppsFlyerReferralParamsHandler): AppsFlyerConversionDataHandler
}