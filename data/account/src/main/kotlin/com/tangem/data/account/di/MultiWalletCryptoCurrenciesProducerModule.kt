package com.tangem.data.account.di

import com.tangem.data.account.producer.AccountListCryptoCurrenciesProducer
import com.tangem.data.account.producer.DefaultMultiWalletCryptoCurrenciesProducer
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
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
        accountsFeatureToggles: AccountsFeatureToggles,
        defaultImpl: DefaultMultiWalletCryptoCurrenciesProducer.Factory,
        accountsImpl: AccountListCryptoCurrenciesProducer.Factory,
    ): MultiWalletCryptoCurrenciesProducer.Factory {
        return if (accountsFeatureToggles.isFeatureEnabled) accountsImpl else defaultImpl
    }
}