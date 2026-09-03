package com.tangem.datasource.local.datastore.di

import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.local.datastore.DefaultAppDataStoreFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppDataStoreFactoryModule {

    // Not scoped: the factory is stateless (no caches, no init work), so binding it unscoped avoids the
    // DoubleCheck provider overhead — creating it on demand is cheaper than the per-access singleton checks.
    @Binds
    fun bindAppDataStoreFactory(impl: DefaultAppDataStoreFactory): AppDataStoreFactory
}