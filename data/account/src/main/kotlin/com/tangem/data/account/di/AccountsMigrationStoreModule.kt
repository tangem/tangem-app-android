package com.tangem.data.account.di

import com.tangem.data.account.store.AccountTokenMigrationStore
import com.tangem.data.account.store.DefaultAccountTokenMigrationStore
import com.tangem.core.local.datastore.RuntimeStateStore
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