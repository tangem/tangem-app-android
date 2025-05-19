package com.tangem.features.onramp.deeplink.di

import com.tangem.features.onramp.deeplink.*
import com.tangem.features.onramp.deeplink.DefaultOnrampDeepLink
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampDeeplinkModule {

    @Binds
    @Singleton
    fun bindFactory(impl: DefaultOnrampDeepLink.Factory): OnrampDeepLink.Factory

    @Binds
    @Singleton
    fun bindFactoryV2(impl: DefaultOnrampDeepLinkHandler.Factory): OnrampDeepLinkHandler.Factory
}