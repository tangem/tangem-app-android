package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.DefaultPolymarketRepository
import com.tangem.data.polymarket.derivation.DefaultPolymarketDepositWalletDeriver
import com.tangem.data.polymarket.derivation.DefaultPolymarketEoaDeriver
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
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

    @Binds
    @Singleton
    fun bindPolymarketDepositWalletDeriver(impl: DefaultPolymarketDepositWalletDeriver): PolymarketDepositWalletDeriver

    @Binds
    @Singleton
    fun bindPolymarketEoaDeriver(impl: DefaultPolymarketEoaDeriver): PolymarketEoaDeriver
}