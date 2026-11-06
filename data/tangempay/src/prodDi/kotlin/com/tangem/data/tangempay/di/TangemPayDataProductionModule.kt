package com.tangem.data.tangempay.di

import com.tangem.data.tangempay.repository.DefaultBankOfframpOrderRepository
import com.tangem.data.tangempay.repository.DefaultBankOfframpRepository
import com.tangem.data.tangempay.repository.DefaultCashbackRepository
import com.tangem.data.tangempay.repository.DefaultCustomerOffersRepository
import com.tangem.data.tangempay.repository.DefaultCustomerSummaryRepository
import com.tangem.data.tangempay.repository.DefaultCustomerOrderRepository
import com.tangem.data.tangempay.repository.DefaultOnboardingRepository
import com.tangem.data.tangempay.repository.DefaultTangemPayCampaignRepository
import com.tangem.data.tangempay.repository.DefaultTangemPayCardDetailsRepository
import com.tangem.data.tangempay.repository.DefaultTangemPayTxHistoryRepository
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
internal interface TangemPayDataProductionModule {

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: DefaultOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: DefaultTangemPayCardDetailsRepository): TangemPayCardDetailsRepository

    @Binds
    @Singleton
    fun bindCashbackRepository(repository: DefaultCashbackRepository): CashbackRepository

    @Binds
    @Singleton
    fun bindTxHistoryRepository(repository: DefaultTangemPayTxHistoryRepository): TangemPayTxHistoryRepository

    @Binds
    @Singleton
    fun bindCustomerOrderRepository(repository: DefaultCustomerOrderRepository): CustomerOrderRepository

    @Binds
    @Singleton
    fun bindCustomerOffersRepository(repository: DefaultCustomerOffersRepository): CustomerOffersRepository

    @Binds
    @Singleton
    fun bindTangemPayCampaignRepository(repository: DefaultTangemPayCampaignRepository): TangemPayCampaignRepository

    @Binds
    @Singleton
    fun bindBankOfframpOrderRepository(repository: DefaultBankOfframpOrderRepository): BankOfframpOrderRepository

    @Binds
    @Singleton
    fun bindBankOfframpRepository(repository: DefaultBankOfframpRepository): BankOfframpRepository

    @Binds
    @Singleton
    fun bindCustomerSummaryRepository(repository: DefaultCustomerSummaryRepository): CustomerSummaryRepository
}