package com.tangem.feature.referral.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.feature.referral.deeplink.DefaultReferralDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ReferralDeepLinkModule {

    @Binds
    @Singleton
    fun bindFactory(impl: DefaultReferralDeepLinkHandler.Factory): ReferralDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultReferralDeepLinkHandler.DEEPLINK_KEY)
        fun provideReferralDeepLinkHandler(factory: ReferralDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { factory.create() }
    }
}