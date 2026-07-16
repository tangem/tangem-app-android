package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.mock.MockPolymarketRepository
import com.tangem.domain.polymarket.PolymarketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketDataModule {

    // UI-first: fixtures are served while the feature is being built.
    // DefaultPolymarketRepository is the real BFF-backed impl, swapped in once the contract stabilizes.
    @Binds
    @Singleton
    fun bindPolymarketRepository(impl: MockPolymarketRepository): PolymarketRepository
}