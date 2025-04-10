package com.tangem.blockchainsdk.di

import com.tangem.blockchainsdk.providers.BlockchainProvidersResponseMerger
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExcludedBlockchainsModule {

    @Provides
    @Singleton
    fun bindExcludedBlockchains(excludedBlockchainsManager: ExcludedBlockchainsManager): ExcludedBlockchains {
        return ExcludedBlockchains(excludedBlockchainsManager)
    }

    @Provides
    @Singleton
    fun bindBlockchainProvidersResponseMerger(
        analyticsExceptionHandler: AnalyticsExceptionHandler,
    ): BlockchainProvidersResponseMerger {
        return BlockchainProvidersResponseMerger(analyticsExceptionHandler)
    }
}