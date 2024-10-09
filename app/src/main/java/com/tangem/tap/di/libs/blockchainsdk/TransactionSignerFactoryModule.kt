package com.tangem.tap.di.libs.blockchainsdk

import com.tangem.blockchainsdk.signer.TransactionSignerFactory
import com.tangem.tap.common.libs.blockchainsdk.DefaultTransactionSignerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author Andrew Khokhlov on 09/10/2024
 */
@Module
@InstallIn(SingletonComponent::class)
internal class TransactionSignerFactoryModule {

    @Provides
    @Singleton
    fun provideTransactionSignerFactory(): TransactionSignerFactory {
        return DefaultTransactionSignerFactory()
    }
}
