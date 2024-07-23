package com.tangem.feature.swap.domain.di

import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.GetCardTokensListUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.*
import com.tangem.feature.swap.domain.api.SwapRepository
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
        @SwapScope getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        getCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusesSyncUseCase,
        @SwapScope sendTransactionUseCase: SendTransactionUseCase,
        @SwapScope createTransactionUseCase: CreateTransactionUseCase,
        createTransactionDataExtrasUseCase: CreateTransactionDataExtrasUseCase,
        isDemoCardUseCase: IsDemoCardUseCase,
        quotesRepository: QuotesRepository,
        swapTransactionRepository: SwapTransactionRepository,
        appCurrencyRepository: AppCurrencyRepository,
        currencyChecksRepository: CurrencyChecksRepository,
        initialToCurrencyResolver: InitialToCurrencyResolver,
        currenciesRepository: CurrenciesRepository,
        validateTransactionUseCase: ValidateTransactionUseCase,
        estimateFeeUseCase: EstimateFeeUseCase,
    ): SwapInteractor {
        return SwapInteractorImpl(
            transactionManager = transactionManager,
            userWalletManager = userWalletManager,
            repository = swapRepository,
            allowPermissionsHandler = AllowPermissionsHandlerImpl(),
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            getMultiCryptoCurrencyStatusUseCase = getCryptoCurrencyStatusUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            createTransactionUseCase = createTransactionUseCase,
            createTransactionExtrasUseCase = createTransactionDataExtrasUseCase,
            isDemoCardUseCase = isDemoCardUseCase,
            quotesRepository = quotesRepository,
            swapTransactionRepository = swapTransactionRepository,
            appCurrencyRepository = appCurrencyRepository,
            currencyChecksRepository = currencyChecksRepository,
            currenciesRepository = currenciesRepository,
            initialToCurrencyResolver = initialToCurrencyResolver,
            demoConfig = DemoConfig(),
            validateTransactionUseCase = validateTransactionUseCase,
            estimateFeeUseCase = estimateFeeUseCase,
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
    fun providesGetSelectedWalletUseCase(userWalletsListManager: UserWalletsListManager): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesGetCryptoCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        stakingRepository: StakingRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyStatusesSyncUseCase {
        return GetCryptoCurrencyStatusesSyncUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
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
        stakingRepository: StakingRepository,
    ): GetCardTokensListUseCase {
        return GetCardTokensListUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
        )
    }

    @SwapScope
    @Provides
    fun provideDemoCardUseCase(): IsDemoCardUseCase {
        return IsDemoCardUseCase(config = DemoConfig())
    }

    @SwapScope
    @Provides
    @Singleton
    fun provideCreateTransactionUseCase(transactionRepository: TransactionRepository): CreateTransactionUseCase {
        return CreateTransactionUseCase(
            transactionRepository = transactionRepository,
        )
    }

    @SwapScope
    @Provides
    @Singleton
    fun provideSendTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        transactionRepository: TransactionRepository,
        walletManagersFacade: WalletManagersFacade,
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            demoConfig = DemoConfig(),
            cardSdkConfigRepository = cardSdkConfigRepository,
            transactionRepository = transactionRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideInitialToCurrencyResolver(
        @SwapScope getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        swapTransactionRepository: SwapTransactionRepository,
    ): InitialToCurrencyResolver {
        return DefaultInitialToCurrencyResolver(
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            swapTransactionRepository = swapTransactionRepository,
        )
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SwapScope
