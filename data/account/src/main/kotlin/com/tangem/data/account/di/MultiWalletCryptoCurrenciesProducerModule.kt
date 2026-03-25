package com.tangem.data.account.di

import com.tangem.data.account.producer.AccountListCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MultiWalletCryptoCurrenciesProducerModule {

    @Singleton
    @Provides
    fun provideMultiWalletCryptoCurrenciesProducerFactory(
        accountsImpl: AccountListCryptoCurrenciesProducer.Factory,
    ): MultiWalletCryptoCurrenciesProducer.Factory {
        return accountsImpl
    }
}