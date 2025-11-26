package com.tangem.data.pay.di

import com.tangem.data.pay.DefaultTangemPayTopUpDataFactory
import com.tangem.data.pay.repository.DefaultKycRepository
import com.tangem.data.pay.repository.DefaultOnboardingRepository
import com.tangem.data.pay.repository.DefaultTangemPayCardDetailsRepository
import com.tangem.data.pay.repository.DefaultTangemPayTxHistoryRepository
import com.tangem.data.pay.usecase.DefaultGetTangemPayCurrencyStatusUseCase
import com.tangem.domain.pay.TangemPayTopUpDataFactory
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
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
    fun bindCardDetailsRepository(repository: DefaultTangemPayCardDetailsRepository): TangemPayCardDetailsRepository

    @Binds
    @Singleton
    fun bindDataForReceiveFactory(factory: DefaultTangemPayTopUpDataFactory): TangemPayTopUpDataFactory

    @Binds
    @Singleton
    fun bindGetTangemPayCurrencyStatusUseCase(
        impl: DefaultGetTangemPayCurrencyStatusUseCase,
    ): GetTangemPayCurrencyStatusUseCase

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
        fun provideProduceTangemPayInitialDataUseCase(
            repository: OnboardingRepository,
        ): ProduceTangemPayInitialDataUseCase {
            return ProduceTangemPayInitialDataUseCase(repository = repository)
        }
    }
}