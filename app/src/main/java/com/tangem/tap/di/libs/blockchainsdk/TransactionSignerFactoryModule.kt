package com.tangem.tap.di.libs.blockchainsdk

import com.tangem.core.analytics.store.LastSignedWalletFormStore
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.tap.common.libs.blockchainsdk.DefaultTransactionSignerFactory
import com.tangem.utils.coroutines.AppCoroutineScope
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
        userWalletsListRepository: UserWalletsListRepository,
        appCoroutineScope: AppCoroutineScope,
    ): TransactionSignerFactory {
        return DefaultTransactionSignerFactory(
            lastSignedWalletFormStore = lastSignedWalletFormStore,
            userWalletsListRepository = userWalletsListRepository,
            coroutineScope = appCoroutineScope,
        )
    }
}