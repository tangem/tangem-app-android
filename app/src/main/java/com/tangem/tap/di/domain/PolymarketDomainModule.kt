package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
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
    fun provideGetPolymarketEventsUseCase(): GetPolymarketEventsUseCase = GetPolymarketEventsUseCase()

    @Provides
    @Singleton
    fun provideCheckPolymarketGeoblockUseCase(
        polymarketRepository: PolymarketRepository,
    ): CheckPolymarketGeoblockUseCase = CheckPolymarketGeoblockUseCase(polymarketRepository = polymarketRepository)

    @Provides
    @Singleton
    fun provideGetPolymarketRelayerNonceUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketRelayerNonceUseCase = GetPolymarketRelayerNonceUseCase(
        polymarketRepository = polymarketRepository,
    )

    @Provides
    @Singleton
    fun provideDerivePolymarketAddressesUseCase(
        eoaDeriver: PolymarketEoaDeriver,
        depositWalletDeriver: PolymarketDepositWalletDeriver,
    ): DerivePolymarketAddressesUseCase = DerivePolymarketAddressesUseCase(
        eoaDeriver = eoaDeriver,
        depositWalletDeriver = depositWalletDeriver,
    )

    @Provides
    @Singleton
    fun provideGetPolymarketWalletStatusUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketWalletStatusUseCase = GetPolymarketWalletStatusUseCase(
        polymarketRepository = polymarketRepository,
    )
}