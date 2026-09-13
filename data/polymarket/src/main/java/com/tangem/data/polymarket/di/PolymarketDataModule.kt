package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.DefaultPolymarketRepository
import com.tangem.data.polymarket.entity.DefaultPolymarketCollateralCurrencyFactory
import com.tangem.data.polymarket.derivation.DefaultPolymarketDepositWalletDeriver
import com.tangem.data.polymarket.derivation.DefaultPolymarketEoaDeriver
import com.tangem.data.polymarket.flow.DefaultPredictionAccountStatusFetcher
import com.tangem.data.polymarket.flow.DefaultPredictionAccountStatusProducer
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionAccountStatusProducer
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import dagger.Binds
import dagger.Module
import dagger.Provides
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
    fun bindPolymarketCollateralCurrencyFactory(
        impl: DefaultPolymarketCollateralCurrencyFactory,
    ): PolymarketCollateralCurrencyFactory

    @Binds
    @Singleton
    fun bindPolymarketDepositWalletDeriver(impl: DefaultPolymarketDepositWalletDeriver): PolymarketDepositWalletDeriver

    @Binds
    @Singleton
    fun bindPolymarketEoaDeriver(impl: DefaultPolymarketEoaDeriver): PolymarketEoaDeriver

    @Binds
    @Singleton
    fun bindPredictionAccountStatusProducerFactory(
        impl: DefaultPredictionAccountStatusProducer.Factory,
    ): PredictionAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindPredictionAccountStatusFetcher(impl: DefaultPredictionAccountStatusFetcher): PredictionAccountStatusFetcher

    companion object {

        @Provides
        @Singleton
        fun providePredictionAccountStatusSupplier(
            factory: PredictionAccountStatusProducer.Factory,
        ): PredictionAccountStatusSupplier {
            return object : PredictionAccountStatusSupplier(
                factory = factory,
                keyCreator = { params -> "prediction_account_status_${params.userWalletId.stringValue}" },
            ) {}
        }
    }
}