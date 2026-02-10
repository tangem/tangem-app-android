package com.tangem.tap.di.domain

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.markets.*
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    fun provideSaveMarketTokensUseCase(
        derivationsRepository: DerivationsRepository,
        marketsTokenRepository: MarketsTokenRepository,
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): SaveMarketTokensUseCase {
        return SaveMarketTokensUseCase(
            derivationsRepository = derivationsRepository,
            marketsTokenRepository = marketsTokenRepository,
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
            parallelUpdatingScope = CoroutineScope(SupervisorJob() + dispatchers.default),
        )
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

    @Provides
    @Singleton
    fun provideShouldShowYieldModeMarketPromoUseCase(
        promoRepository: PromoRepository,
        marketsTokenRepository: MarketsTokenRepository,
    ): ShouldShowYieldModeMarketPromoUseCase {
        return ShouldShowYieldModeMarketPromoUseCase(
            promoRepository = promoRepository,
            marketsTokenRepository = marketsTokenRepository,
        )
    }
}