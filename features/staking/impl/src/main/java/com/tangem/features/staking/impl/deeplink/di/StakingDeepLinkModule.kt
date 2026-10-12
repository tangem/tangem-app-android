package com.tangem.features.staking.impl.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import com.tangem.features.staking.impl.deeplink.DefaultStakingDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingDeepLinkModule {
    @Binds
    @Singleton
    fun bindStakingDeepLinkHandlerFactory(impl: DefaultStakingDeepLinkHandler.Factory): StakingDeepLinkHandler.Factory

    companion object {

        // Temporary bridge to the legacy factory: once the legacy routing is removed, the handler itself must be
        // bound here as a DeepLinkHandler instead of a lambda over its factory.
        @Provides
        @IntoMap
        @DeepLinkKey(DefaultStakingDeepLinkHandler.DEEPLINK_KEY)
        fun provideStakingDeepLinkHandler(factory: StakingDeepLinkHandler.Factory): DeepLinkHandler =
            DeepLinkHandler { request ->
                factory.create(coroutineScope = request.scope, queryParams = request.queryParams)
            }
    }
}