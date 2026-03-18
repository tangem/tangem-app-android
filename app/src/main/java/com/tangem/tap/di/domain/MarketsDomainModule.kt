package com.tangem.tap.di.domain

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MarketsDomainModule {

    @Provides
    @Singleton
    fun provideGetMarketsTokenListFlowUseCase(
        marketsTokenRepository: MarketsTokenRepository,
    ): GetMarketsTokenListFlowUseCase {
        return GetMarketsTokenListFlowUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideGetTopFiveMarketTokenUseCase(
        marketsTokenRepository: MarketsTokenRepository,
    ): GetTopFiveMarketTokenUseCase {
        return GetTopFiveMarketTokenUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideGetTokenPriceChartUseCase(marketsTokenRepository: MarketsTokenRepository): GetTokenPriceChartUseCase {
        return GetTokenPriceChartUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideGetTokenMarketInfoUseCase(marketsTokenRepository: MarketsTokenRepository): GetTokenMarketInfoUseCase {
        return GetTokenMarketInfoUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideTokenFullQuotesUseCase(marketsTokenRepository: MarketsTokenRepository): GetTokenFullQuotesUseCase {
        return GetTokenFullQuotesUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideGetTokenQuotesUseCase(singleQuoteStatusSupplier: SingleQuoteStatusSupplier): GetCurrencyQuotesUseCase {
        return GetCurrencyQuotesUseCase(singleQuoteStatusSupplier = singleQuoteStatusSupplier)
    }

    @Provides
    @Singleton
    fun provideGetTokenMarketCryptoCurrency(
        marketsTokenRepository: MarketsTokenRepository,
    ): GetTokenMarketCryptoCurrency {
        return GetTokenMarketCryptoCurrency(
            marketsTokenRepository = marketsTokenRepository,
        )
    }

    @Provides
    @Singleton
    fun provideFilterNetworksUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        excludedBlockchains: ExcludedBlockchains,
    ): FilterAvailableNetworksForWalletUseCase {
        return FilterAvailableNetworksForWalletUseCase(
            userWalletsListRepository = userWalletsListRepository,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideGetTokenExchangesUseCase(marketsTokenRepository: MarketsTokenRepository): GetTokenExchangesUseCase {
        return GetTokenExchangesUseCase(marketsTokenRepository = marketsTokenRepository)
    }
}