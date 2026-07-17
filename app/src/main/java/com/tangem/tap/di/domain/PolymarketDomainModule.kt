package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketDomainModule {

    @Provides
    @Singleton
    fun provideGetPolymarketEventsUseCase(polymarketRepository: PolymarketRepository): GetPolymarketEventsUseCase {
        return GetPolymarketEventsUseCase(polymarketRepository = polymarketRepository)
    }
}