package com.tangem.feature.referral.deeplink.di

import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.feature.referral.deeplink.DefaultReferralDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ReferralDeepLinkModule {

    @Binds
    @Singleton
    fun bindFactory(impl: DefaultReferralDeepLinkHandler.Factory): ReferralDeepLinkHandler.Factory
}