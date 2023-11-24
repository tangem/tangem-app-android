package com.tangem.feature.swap.domain.di

import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.*
import com.tangem.feature.swap.domain.cache.SwapDataCacheImpl
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SwapDomainModule {

    @Provides
    @Singleton
    fun provideSwapInteractor(
        swapRepository: SwapRepository,
        userWalletManager: UserWalletManager,
        transactionManager: TransactionManager,
        currenciesRepository: CurrenciesRepository,
        networksRepository: NetworksRepository,
        walletFeatureToggles: WalletFeatureToggles,
        @SwapScope getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        @SwapScope getCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusesSyncUseCase,
        quotesRepository: QuotesRepository,
        walletManagersFacade: WalletManagersFacade,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): SwapInteractor {
        return SwapInteractorImpl(
            transactionManager = transactionManager,
            userWalletManager = userWalletManager,
            repository = swapRepository,
            cache = SwapDataCacheImpl(),
            allowPermissionsHandler = AllowPermissionsHandlerImpl(),
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            walletFeatureToggles = walletFeatureToggles,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            getMultiCryptoCurrencyStatusUseCase = getCryptoCurrencyStatusUseCase,
            quotesRepository = quotesRepository,
            walletManagersFacade = walletManagersFacade,
            dispatcher = coroutineDispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainInteractor(transactionManager: TransactionManager): BlockchainInteractor {
        return DefaultBlockchainInteractor(
            transactionManager = transactionManager,
        )
    }

    @SwapScope
    @Provides
    @Singleton
    fun providesGetSelectedWalletUseCase(walletsStateHolder: WalletsStateHolder): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(walletsStateHolder = walletsStateHolder)
    }

    @SwapScope
    @Provides
    @Singleton
    fun providesGetCryptoCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyStatusesSyncUseCase {
        return GetCryptoCurrencyStatusesSyncUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            dispatchers = dispatchers,
        )
    }

    @SwapScope
    @Provides
    @Singleton
    fun providesGetCardTokensListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCardTokensListUseCase {
        return GetCardTokensListUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            dispatchers = dispatchers,
        )
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SwapScope
