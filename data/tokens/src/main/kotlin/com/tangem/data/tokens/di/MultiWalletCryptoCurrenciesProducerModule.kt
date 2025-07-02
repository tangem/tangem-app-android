package com.tangem.data.tokens.di

import com.tangem.data.tokens.DefaultMultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MultiWalletCryptoCurrenciesProducerModule {

    @Singleton
    @Binds
    fun bindMultiWalletCryptoCurrenciesProducerFactory(
        impl: DefaultMultiWalletCryptoCurrenciesProducer.Factory,
    ): MultiWalletCryptoCurrenciesProducer.Factory
}