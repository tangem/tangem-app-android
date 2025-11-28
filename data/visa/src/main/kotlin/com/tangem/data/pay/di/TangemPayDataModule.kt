package com.tangem.data.pay.di

import com.tangem.data.pay.DefaultTangemPaySwapDataFactory
import com.tangem.data.pay.repository.*
import com.tangem.data.pay.usecase.DefaultGetTangemPayCurrencyStatusUseCase
import com.tangem.data.pay.usecase.DefaultTangemPayWithdrawUseCase
import com.tangem.domain.pay.TangemPaySwapDataFactory
import com.tangem.domain.pay.repository.*
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
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
    fun bindTangemPaySwapRepository(repository: DefaultTangemPaySwapRepository): TangemPaySwapRepository

    @Binds
    @Singleton
    fun bindCustomerOrderRepository(repository: DefaultCustomerOrderRepository): CustomerOrderRepository

    @Binds
    @Singleton
    fun bindTangemPaySwapDataFactory(factory: DefaultTangemPaySwapDataFactory): TangemPaySwapDataFactory

    @Binds
    @Singleton
    fun bindGetTangemPayCurrencyStatusUseCase(
        impl: DefaultGetTangemPayCurrencyStatusUseCase,
    ): GetTangemPayCurrencyStatusUseCase

    @Binds
    @Singleton
    fun bindTangemPayWithdrawUseCase(impl: DefaultTangemPayWithdrawUseCase): TangemPayWithdrawUseCase

    companion object {
        @Provides
        @Singleton
        fun provideTangemPayMainScreenCustomerInfoUseCase(
            repository: OnboardingRepository,
            customerOrderRepository: CustomerOrderRepository,
        ): TangemPayMainScreenCustomerInfoUseCase {
            return TangemPayMainScreenCustomerInfoUseCase(
                repository = repository,
                customerOrderRepository = customerOrderRepository,
            )
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