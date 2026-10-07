package com.tangem.data.pay.di

import com.tangem.data.pay.repository.DefaultCustomerOrderRepository
import com.tangem.data.pay.repository.MockAwareBankOfframpOrderRepository
import com.tangem.data.pay.repository.MockAwareCashbackRepository
import com.tangem.data.pay.repository.MockAwareCustomerOffersRepository
import com.tangem.data.pay.repository.MockAwareOnboardingRepository
import com.tangem.data.pay.repository.MockAwareTangemPayCampaignRepository
import com.tangem.data.pay.repository.MockAwareTangemPayCardDetailsRepository
import com.tangem.data.pay.repository.MockAwareTangemPayTxHistoryRepository
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataMockedModule {

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: MockAwareOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: MockAwareTangemPayCardDetailsRepository): TangemPayCardDetailsRepository

    @Binds
    @Singleton
    fun bindCashbackRepository(repository: MockAwareCashbackRepository): CashbackRepository

    @Binds
    @Singleton
    fun bindTxHistoryRepository(repository: MockAwareTangemPayTxHistoryRepository): TangemPayTxHistoryRepository

    @Binds
    @Singleton
    fun bindCustomerOrderRepository(repository: DefaultCustomerOrderRepository): CustomerOrderRepository

    @Binds
    @Singleton
    fun bindCustomerOffersRepository(repository: MockAwareCustomerOffersRepository): CustomerOffersRepository

    @Binds
    @Singleton
    fun bindTangemPayCampaignRepository(
        repository: MockAwareTangemPayCampaignRepository,
    ): TangemPayCampaignRepository

    @Binds
    @Singleton
    fun bindBankOfframpOrderRepository(
        repository: MockAwareBankOfframpOrderRepository,
    ): BankOfframpOrderRepository
}