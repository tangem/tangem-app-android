package com.tangem.data.wallets.di

import com.tangem.data.wallets.DefaultWalletsRepository
import com.tangem.datasource.local.userwallet.ShouldSaveUserWalletStore
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletsDataModule {

    @Provides
    @Singleton
    fun providesWalletsRepository(
        shouldSaveUserWalletStore: ShouldSaveUserWalletStore,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): WalletsRepository {
        return DefaultWalletsRepository(
            shouldSaveUserWalletStore = shouldSaveUserWalletStore,
            dispatchers = coroutineDispatcherProvider,
        )
    }
}