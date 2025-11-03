package com.tangem.data.pay.di

import com.tangem.data.pay.DefaultDataForReceiveFactory
import com.tangem.data.pay.repository.DefaultCardDetailsRepository
import com.tangem.data.pay.repository.DefaultKycRepository
import com.tangem.data.pay.repository.DefaultTangemPayTxHistoryRepository
import com.tangem.data.pay.repository.DefaultOnboardingRepository
import com.tangem.domain.pay.DataForReceiveFactory
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.TangemPayIssueOrderUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataModule {

    @Binds
    @Singleton
    fun bindKycRepository(repository: DefaultKycRepository): KycRepository

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: DefaultOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindTangemPayTxHistoryRepository(repository: DefaultTangemPayTxHistoryRepository): TangemPayTxHistoryRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: DefaultCardDetailsRepository): CardDetailsRepository

    @Binds
    @Singleton
    fun bindDataForReceiveFactory(factory: DefaultDataForReceiveFactory): DataForReceiveFactory

    companion object {
        @Provides
        @Singleton
        fun provideTangemPayMainScreenCustomerInfoUseCase(
            repository: OnboardingRepository,
        ): TangemPayMainScreenCustomerInfoUseCase {
            return TangemPayMainScreenCustomerInfoUseCase(repository = repository)
        }

        @Provides
        @Singleton
        fun provideTangemPayIssueOrderUseCase(repository: OnboardingRepository): TangemPayIssueOrderUseCase {
            return TangemPayIssueOrderUseCase(repository = repository)
        }
    }
}