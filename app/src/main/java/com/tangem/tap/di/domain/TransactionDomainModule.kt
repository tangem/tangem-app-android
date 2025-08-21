package com.tangem.tap.di.domain

import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
            demoConfig = DemoConfig(),
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
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            demoConfig = DemoConfig(),
            cardSdkConfigRepository = cardSdkConfigRepository,
            transactionRepository = transactionRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideAssociateAssetUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        tokensFeatureToggles: TokensFeatureToggles,
    ): AssociateAssetUseCase {
        return AssociateAssetUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            tokensFeatureToggles = tokensFeatureToggles,
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
            demoConfig = DemoConfig(),
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
    fun provideIsUtxoConsolidationAvailableUseCase(
        walletManagersFacade: WalletManagersFacade,
    ): IsUtxoConsolidationAvailableUseCase {
        return IsUtxoConsolidationAvailableUseCase(walletManagersFacade)
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
    ): PrepareAndSignUseCase {
        return PrepareAndSignUseCase(transactionRepository, cardSdkConfigRepository)
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
}