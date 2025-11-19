package com.tangem.data.hotwallet.di

import com.tangem.data.hotwallet.DefaultHotWalletRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HotWalletDataModule {

    @Provides
    @Singleton
    fun provideHotWalletRepository(appPreferencesStore: AppPreferencesStore): HotWalletRepository {
        return DefaultHotWalletRepository(
            appPreferencesStore = appPreferencesStore,
        )
    }
}