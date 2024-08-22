package com.tangem.tap.di.domain

import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.tokens.repository.QuotesRepository
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
    fun provideGetTokenQuotesUseCase(quotesRepository: QuotesRepository): GetTokenQuotesUseCase {
        return GetTokenQuotesUseCase(quotesRepository = quotesRepository)
    }
}