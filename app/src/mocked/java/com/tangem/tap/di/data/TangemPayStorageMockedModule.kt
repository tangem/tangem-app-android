package com.tangem.tap.di.data

import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.tap.data.MockAwareTangemPayStorage
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