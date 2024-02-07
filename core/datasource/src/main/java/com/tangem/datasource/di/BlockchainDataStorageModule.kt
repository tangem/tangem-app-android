package com.tangem.datasource.di

import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.datasource.local.blockchain.DefaultBlockchainDataStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockchainDataStorageModule {

    @Provides
    @Singleton
    fun provideBlockchainDataStorage(appPreferencesStore: AppPreferencesStore): BlockchainDataStorage {
        return DefaultBlockchainDataStorage(appPreferencesStore)
    }
}