package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.visa.DefaultTangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemPayStoresModule {

    @Provides
    @Singleton
    fun provideTangemPayCardFrozenStateStore(): TangemPayCardFrozenStateStore {
        return DefaultTangemPayCardFrozenStateStore(
            dataStore = RuntimeDataStore(),
        )
    }
}