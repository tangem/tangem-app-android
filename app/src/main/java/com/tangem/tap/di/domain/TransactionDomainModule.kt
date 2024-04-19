package com.tangem.tap.di.domain

import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.IsDemoCardUseCase
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
    fun provideGetFeeUseCase(
        walletManagersFacade: WalletManagersFacade,
        isDemoCardUseCase: IsDemoCardUseCase,
    ): GetFeeUseCase {
        return GetFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            isDemoCardUseCase = isDemoCardUseCase,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideSendTransactionUseCase(
        isDemoCardUseCase: IsDemoCardUseCase,
        cardSdkConfigRepository: CardSdkConfigRepository,
        transactionRepository: TransactionRepository,
        walletManagersFacade: WalletManagersFacade,
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            isDemoCardUseCase = isDemoCardUseCase,
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
