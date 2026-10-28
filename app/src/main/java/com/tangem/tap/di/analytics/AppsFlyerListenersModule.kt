package com.tangem.tap.di.analytics

import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.deeplink.DeepLinkListener
import com.tangem.tap.common.analytics.appsflyer.AppsFlyerDeepLinkListener
import com.tangem.tap.common.analytics.appsflyer.TangemAFConversionListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppsFlyerListenersModule {

    @Binds
    fun bindDeepLinkListener(listener: AppsFlyerDeepLinkListener): DeepLinkListener

    @Binds
    fun bindConversionListener(listener: TangemAFConversionListener): AppsFlyerConversionListener
}