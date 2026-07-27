package com.tangem.data.pay.di

import com.tangem.data.pay.repository.DefaultCardDeliveryQuoteRepository
import com.tangem.data.pay.repository.DefaultCashbackRepository
import com.tangem.data.pay.repository.DefaultOnboardingRepository
import com.tangem.data.pay.repository.DefaultTangemPayCardDetailsRepository
import com.tangem.data.pay.repository.DefaultTangemPayTxHistoryRepository
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
    fun bindCardDeliveryQuoteRepository(repository: DefaultCardDeliveryQuoteRepository): CardDeliveryQuoteRepository
}