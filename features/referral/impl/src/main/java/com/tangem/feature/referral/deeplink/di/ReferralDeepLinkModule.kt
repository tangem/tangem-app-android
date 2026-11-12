package com.tangem.feature.referral.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.referral.deeplink.ReferralDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ReferralDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(ReferralDeepLinkHandler.DEEPLINK_KEY)
    fun bindReferralDeepLinkHandler(impl: ReferralDeepLinkHandler): DeepLinkHandler
}