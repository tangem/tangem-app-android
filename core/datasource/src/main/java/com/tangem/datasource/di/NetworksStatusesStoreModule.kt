package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.DefaultNetworksStatusesStore
import com.tangem.datasource.local.network.NetworksStatusesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object NetworksStatusesStoreModule {

    @Provides
    fun provideNetworksStatusesStore(): NetworksStatusesStore {
        return DefaultNetworksStatusesStore(
            dataStore = RuntimeDataStore(),
        )
    }
}