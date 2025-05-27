package com.tangem.features.staking.impl.deeplink.di

import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import com.tangem.features.staking.impl.deeplink.DefaultStakingDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingDeepLinkModule {
    @Binds
    @Singleton
    fun bindStakingDeepLinkHandlerFactory(impl: DefaultStakingDeepLinkHandler.Factory): StakingDeepLinkHandler.Factory
}