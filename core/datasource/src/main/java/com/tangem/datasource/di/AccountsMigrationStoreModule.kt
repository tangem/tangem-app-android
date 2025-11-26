package com.tangem.datasource.di

import com.tangem.datasource.local.accounts.AccountTokenMigrationStore
import com.tangem.datasource.local.accounts.DefaultAccountTokenMigrationStore
import com.tangem.datasource.local.datastore.RuntimeStateStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountsMigrationStoreModule {

    @Provides
    @Singleton
    fun provideAccountTokenMigrationStore(): AccountTokenMigrationStore {
        return DefaultAccountTokenMigrationStore(
            runtimeStateStore = RuntimeStateStore(emptyMap()),
        )
    }
}