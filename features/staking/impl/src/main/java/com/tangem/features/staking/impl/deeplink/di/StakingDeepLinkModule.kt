package com.tangem.features.staking.impl.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.staking.impl.deeplink.StakingDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(StakingDeepLinkHandler.DEEPLINK_KEY)
    fun bindStakingDeepLinkHandler(impl: StakingDeepLinkHandler): DeepLinkHandler
}