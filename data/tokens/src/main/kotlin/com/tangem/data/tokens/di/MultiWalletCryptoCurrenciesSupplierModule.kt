package com.tangem.data.tokens.di

import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class MultiWalletCryptoCurrenciesSupplierModule {

    @Singleton
    @Provides
    fun provideMultiWalletCryptoCurrenciesSupplier(
        factory: MultiWalletCryptoCurrenciesProducer.Factory,
    ): MultiWalletCryptoCurrenciesSupplier {
        return object : MultiWalletCryptoCurrenciesSupplier(
            factory = factory,
            keyCreator = { "multi_crypto_currency_${it.userWalletId}" },
        ) {}
    }
}