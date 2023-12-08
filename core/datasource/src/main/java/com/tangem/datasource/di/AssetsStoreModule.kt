package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultAssetsStore
import com.tangem.datasource.local.token.AssetsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AssetsStoreModule {

    @Provides
    @Singleton
    fun provideAssetsStore(): AssetsStore {
        return DefaultAssetsStore(dataStore = RuntimeDataStore())
    }
}
