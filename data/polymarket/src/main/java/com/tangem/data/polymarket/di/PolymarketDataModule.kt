package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.DefaultPolymarketRepository
import com.tangem.domain.polymarket.PolymarketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketDataModule {

    @Binds
    @Singleton
    fun bindPolymarketRepository(impl: DefaultPolymarketRepository): PolymarketRepository
}