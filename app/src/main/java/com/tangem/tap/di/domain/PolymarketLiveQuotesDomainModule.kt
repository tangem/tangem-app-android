package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketLiveQuotesDomainModule {

    @Provides
    fun provideObservePolymarketLiveQuotesUseCase(
        repository: PolymarketLiveQuotesRepository,
    ): ObservePolymarketLiveQuotesUseCase = ObservePolymarketLiveQuotesUseCase(repository)
}