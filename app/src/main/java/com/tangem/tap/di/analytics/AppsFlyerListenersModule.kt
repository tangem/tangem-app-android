package com.tangem.tap.di.analytics

import com.appsflyer.deeplink.DeepLinkListener
import com.tangem.core.analytics.api.AppsFlyerConversionDataHandler
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerDeepLinkListener
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
    fun bindDeepLinkListener(listener: AppsFlyerDeepLinkListener): DeepLinkListener

    @Binds
    @IntoSet
    fun bindReferralParamsHandler(handler: AppsFlyerReferralParamsHandler): AppsFlyerConversionDataHandler
}