package com.tangem.data.tangempay.store.di

import com.tangem.data.tangempay.store.MockAwareTangemPayStorage
import com.tangem.data.tangempay.store.TangemPayStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayStorageMockedModule {

    @Binds
    @Singleton
    fun bindTangemPayStorage(impl: MockAwareTangemPayStorage): TangemPayStorage
}