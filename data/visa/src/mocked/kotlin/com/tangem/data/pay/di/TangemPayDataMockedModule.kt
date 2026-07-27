package com.tangem.data.pay.di

import com.tangem.data.pay.repository.MockAwareCardDeliveryQuoteRepository
import com.tangem.data.pay.repository.MockAwareCashbackRepository
import com.tangem.data.pay.repository.MockAwareOnboardingRepository
import com.tangem.data.pay.repository.MockAwareTangemPayCardDetailsRepository
import com.tangem.data.pay.repository.MockAwareTangemPayTxHistoryRepository
import com.tangem.domain.pay.repository.CardDeliveryQuoteRepository
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.OnboardingRepository
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
    fun bindCardDeliveryQuoteRepository(repository: MockAwareCardDeliveryQuoteRepository): CardDeliveryQuoteRepository
}