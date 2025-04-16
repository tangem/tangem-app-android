package com.tangem.tap.di.domain

import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    fun provideSendTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        transactionRepository: TransactionRepository,
        walletManagersFacade: WalletManagersFacade,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        tokensFeatureToggles: TokensFeatureToggles,
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            demoConfig = DemoConfig(),
            cardSdkConfigRepository = cardSdkConfigRepository,
            transactionRepository = transactionRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideAssociateAssetUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        currenciesRepository: CurrenciesRepository,
        networksRepository: NetworksRepository,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        tokensFeatureToggles: TokensFeatureToggles,
    ): AssociateAssetUseCase {
        return AssociateAssetUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
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
    fun providePrepareForSendUseCase(
        transactionRepository: TransactionRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): PrepareForSendUseCase {
        return PrepareForSendUseCase(transactionRepository, cardSdkConfigRepository)
    }

    @Provides
    @Singleton
    fun provideSignUseCase(
        walletManagersFacade: WalletManagersFacade,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): SignUseCase {
        return SignUseCase(cardSdkConfigRepository, walletManagersFacade)
    }
}