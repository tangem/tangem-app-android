package com.tangem.tap.di.domain

import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.usecase.CreateTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object TransactionDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetFeeUseCase(walletManagersFacade: WalletManagersFacade): GetFeeUseCase {
        return GetFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig(),
        )
    }

    @Provides
    @ViewModelScoped
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
    @ViewModelScoped
    fun provideCreateTransactionUseCase(transactionRepository: TransactionRepository): CreateTransactionUseCase {
        return CreateTransactionUseCase(transactionRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsFeeApproximateUseCase(feeRepository: FeeRepository): IsFeeApproximateUseCase {
        return IsFeeApproximateUseCase(feeRepository)
    }
}