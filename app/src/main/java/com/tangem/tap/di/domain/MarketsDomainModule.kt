package com.tangem.tap.di.domain

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
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
    fun provideGetTokenQuotesUseCase(
        quotesRepository: QuotesRepository,
        singleQuoteSupplier: SingleQuoteSupplier,
        tokensFeatureToggles: TokensFeatureToggles,
    ): GetCurrencyQuotesUseCase {
        return GetCurrencyQuotesUseCase(
            quotesRepository = quotesRepository,
            singleQuoteSupplier = singleQuoteSupplier,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideSaveMarketTokensUseCase(
        derivationsRepository: DerivationsRepository,
        marketsTokenRepository: MarketsTokenRepository,
        currenciesRepository: CurrenciesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        quotesRepository: QuotesRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteFetcher: MultiQuoteFetcher,
        multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): SaveMarketTokensUseCase {
        return SaveMarketTokensUseCase(
            derivationsRepository = derivationsRepository,
            marketsTokenRepository = marketsTokenRepository,
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            quotesRepository = quotesRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteFetcher = multiQuoteFetcher,
            multiYieldBalanceFetcher = multiYieldBalanceFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideFilterNetworksUseCase(
        userWalletsListManager: UserWalletsListManager,
        excludedBlockchains: ExcludedBlockchains,
    ): FilterAvailableNetworksForWalletUseCase {
        return FilterAvailableNetworksForWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideGetTokenExchangesUseCase(marketsTokenRepository: MarketsTokenRepository): GetTokenExchangesUseCase {
        return GetTokenExchangesUseCase(marketsTokenRepository = marketsTokenRepository)
    }

    @Provides
    @Singleton
    fun provideGetStakingNotificationMaxApyUseCase(
        settingsRepository: SettingsRepository,
        promoRepository: PromoRepository,
        marketsTokenRepository: MarketsTokenRepository,
    ): GetStakingNotificationMaxApyUseCase {
        return GetStakingNotificationMaxApyUseCase(
            settingsRepository = settingsRepository,
            promoRepository = promoRepository,
            marketsTokenRepository = marketsTokenRepository,
        )
    }
}