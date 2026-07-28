package com.tangem.data.pay.store.di

import com.tangem.data.pay.store.DefaultTangemPayStorage
import com.tangem.data.pay.store.TangemPayStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayStorageProductionModule {

    @Binds
    @Singleton
    fun bindTangemPayStorage(impl: DefaultTangemPayStorage): TangemPayStorage
}