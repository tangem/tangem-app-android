package com.tangem.data.wallets.di

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.data.wallets.DefaultWalletsRepository
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
        preferencesDataSource: PreferencesDataSource,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): WalletsRepository {
        return DefaultWalletsRepository(
            preferencesDataSource = preferencesDataSource,
            dispatchers = coroutineDispatcherProvider,
        )
    }
}