package com.tangem.tap.di.libs.blockchainsdk

import com.tangem.core.analytics.store.LastSignedWalletFormStore
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.tap.common.libs.blockchainsdk.DefaultTransactionSignerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal class TransactionSignerFactoryModule {

    @Provides
    @Singleton
    fun provideTransactionSignerFactory(
        lastSignedWalletFormStore: LastSignedWalletFormStore,
    ): TransactionSignerFactory {
        return DefaultTransactionSignerFactory(lastSignedWalletFormStore)
    }
}