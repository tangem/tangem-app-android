package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultExpressAssetsStore
import com.tangem.datasource.local.token.ExpressAssetsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExpressAssetsStoreModule {

    @Provides
    @Singleton
    fun provideExpressAssetsStore(): ExpressAssetsStore {
        return DefaultExpressAssetsStore(dataStore = RuntimeDataStore())
    }
}
