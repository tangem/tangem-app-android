package com.tangem.data.tangempay.di

import com.tangem.data.tangempay.repository.DefaultCustomerOrderRepository
import com.tangem.data.tangempay.repository.MockAwareBankOfframpOrderRepository
import com.tangem.data.tangempay.repository.MockAwareBankOfframpRepository
import com.tangem.data.tangempay.repository.MockAwareCashbackRepository
import com.tangem.data.tangempay.repository.MockAwareCustomerOffersRepository
import com.tangem.data.tangempay.repository.MockAwareCustomerSummaryRepository
import com.tangem.data.tangempay.repository.MockAwareOnboardingRepository
import com.tangem.data.tangempay.repository.MockAwareTangemPayCampaignRepository
import com.tangem.data.tangempay.repository.MockAwareTangemPayCardDetailsRepository
import com.tangem.data.tangempay.repository.MockAwareTangemPayTxHistoryRepository
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import com.tangem.domain.tangempay.repository.BankOfframpRepository
import com.tangem.domain.tangempay.repository.CashbackRepository
import com.tangem.domain.tangempay.repository.CustomerOffersRepository
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.repository.CustomerSummaryRepository
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.repository.TangemPayCampaignRepository
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
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

    @Binds
    @Singleton
    fun bindBankOfframpRepository(repository: MockAwareBankOfframpRepository): BankOfframpRepository

    @Binds
    @Singleton
    fun bindCustomerSummaryRepository(repository: MockAwareCustomerSummaryRepository): CustomerSummaryRepository
}