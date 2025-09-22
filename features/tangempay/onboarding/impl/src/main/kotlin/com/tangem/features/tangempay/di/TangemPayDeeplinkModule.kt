package com.tangem.features.tangempay.di

import com.tangem.features.tangempay.deeplink.DefaultOnboardVisaDeepLinkHandler
import com.tangem.features.tangempay.deeplink.OnboardVisaDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDeeplinkModule {

    @Binds
    @Singleton
    fun bindDeepLinkHandlerFactory(impl: DefaultOnboardVisaDeepLinkHandler.Factory): OnboardVisaDeepLinkHandler.Factory
}