package com.tangem.tap.di.domain

import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.transaction.usecase.gasless.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Module
@InstallIn(SingletonComponent::class)
internal object TransactionDomainModule {

    @Provides
    @Singleton
    fun provideGetFeeUseCase(walletManagersFacade: WalletManagersFacade): GetFeeUseCase {
        return GetFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun provideGetEthSpecificFeeUseCase(walletManagersFacade: WalletManagersFacade): GetEthSpecificFeeUseCase {
        return GetEthSpecificFeeUseCase(walletManagersFacade = walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideSendTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        transactionRepository: TransactionRepository,
        walletManagersFacade: WalletManagersFacade,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
        dispatchers: CoroutineDispatcherProvider,
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            demoConfig = DemoConfig,
            cardSdkConfigRepository = cardSdkConfigRepository,
            transactionRepository = transactionRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            parallelUpdatingScope = CoroutineScope(SupervisorJob() + dispatchers.io),
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideAssociateAssetUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): AssociateAssetUseCase {
        return AssociateAssetUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideRetryTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): RetryIncompleteTransactionUseCase {
        return RetryIncompleteTransactionUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideOpenTrustlineUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): OpenTrustlineUseCase {
        return OpenTrustlineUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideDismissIncompleteTransactionUseCase(
        walletManagersFacade: WalletManagersFacade,
    ): DismissIncompleteTransactionUseCase {
        return DismissIncompleteTransactionUseCase(
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideCreateTransactionUseCase(transactionRepository: TransactionRepository): CreateTransactionUseCase {
        return CreateTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideCreateTransactionExtrasUseCase(
        transactionRepository: TransactionRepository,
    ): CreateTransactionDataExtrasUseCase {
        return CreateTransactionDataExtrasUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideEstimateFeeUseCase(walletManagersFacade: WalletManagersFacade): EstimateFeeUseCase {
        return EstimateFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun provideIsFeeApproximateUseCase(feeRepository: FeeRepository): IsFeeApproximateUseCase {
        return IsFeeApproximateUseCase(feeRepository)
    }

    @Provides
    @Singleton
    fun provideValidateTransactionUseCase(transactionRepository: TransactionRepository): ValidateTransactionUseCase {
        return ValidateTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideIsSelfSendAvailableUseCase(walletManagersFacade: WalletManagersFacade): IsSelfSendAvailableUseCase {
        return IsSelfSendAvailableUseCase(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideCreateApproveTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateApprovalTransactionUseCase {
        return CreateApprovalTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetAllowanceUseCase(transactionRepository: TransactionRepository): GetAllowanceUseCase {
        return GetAllowanceUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideCreateTransferTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateTransferTransactionUseCase {
        return CreateTransferTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun providePrepareForSendUseCase(
        transactionRepository: TransactionRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): PrepareForSendUseCase {
        return PrepareForSendUseCase(
            transactionRepository = transactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun providePrepareAndSignUseCase(
        transactionRepository: TransactionRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): PrepareAndSignUseCase {
        return PrepareAndSignUseCase(
            transactionRepository = transactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun provideSignUseCase(
        walletManagersFacade: WalletManagersFacade,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): SignUseCase {
        return SignUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun provideCreateNFTTransferTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateNFTTransferTransactionUseCase {
        return CreateNFTTransferTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetEnsNameUseCase(
        walletManagersFacade: WalletManagersFacade,
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): GetEnsNameUseCase {
        return GetEnsNameUseCase(
            walletManagersFacade = walletManagersFacade,
            walletAddressServiceRepository = walletAddressServiceRepository,
        )
    }

    @Provides
    @Singleton
    fun provideReceiveAddressesFactory(
        getEnsNameUseCase: GetEnsNameUseCase,
        getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
    ): ReceiveAddressesFactory {
        return ReceiveAddressesFactory(
            getEnsNameUseCase = getEnsNameUseCase,
            getViewedTokenReceiveWarningUseCase = getViewedTokenReceiveWarningUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideGetReverseResolvedEnsAddressUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): GetReverseResolvedEnsAddressUseCase {
        return GetReverseResolvedEnsAddressUseCase(walletAddressServiceRepository)
    }

    @Provides
    @Singleton
    fun sendLargeSolanaTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): SendLargeSolanaTransactionUseCase {
        return SendLargeSolanaTransactionUseCase(cardSdkConfigRepository, walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideGetAvailableFeeTokensUseCase(
        gaslessTransactionRepository: GaslessTransactionRepository,
        currenciesRepository: CurrenciesRepository,
        getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    ): GetAvailableFeeTokensUseCase {
        return GetAvailableFeeTokensUseCase(
            gaslessTransactionRepository = gaslessTransactionRepository,
            currenciesRepository = currenciesRepository,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeeForGaslessUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        currenciesRepository: CurrenciesRepository,
        getFeeUseCase: GetFeeUseCase,
        getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    ): GetFeeForGaslessUseCase {
        return GetFeeForGaslessUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            gaslessTransactionRepository = gaslessTransactionRepository,
            currenciesRepository = currenciesRepository,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            getFeeUseCase = getFeeUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeeForTokenUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        currenciesRepository: CurrenciesRepository,
        getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase,
    ): GetFeeForTokenUseCase {
        return GetFeeForTokenUseCase(
            gaslessTransactionRepository = gaslessTransactionRepository,
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            currenciesRepository = currenciesRepository,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideIsGaslessFeeSupportedForNetwork(
        gaslessTransactionRepository: GaslessTransactionRepository,
    ): IsGaslessFeeSupportedForNetwork {
        return IsGaslessFeeSupportedForNetwork(gaslessTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideCreateAndSendGaslessTransactionUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        getSingCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): CreateAndSendGaslessTransactionUseCase {
        return CreateAndSendGaslessTransactionUseCase(
            walletManagersFacade = walletManagersFacade,
            getSingleCryptoCurrencyStatusUseCase = getSingCryptoCurrencyStatusUseCase,
            gaslessTransactionRepository = gaslessTransactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
        )
    }
}