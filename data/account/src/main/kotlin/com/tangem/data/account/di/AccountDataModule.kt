package com.tangem.data.account.di

import com.tangem.data.account.repository.DefaultAccountsCRUDRepository
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.repository.AccountsCRUDRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDataModule {

    @Provides
    @Singleton
    fun provideAccountsCRUDRepository(userWalletsStore: UserWalletsStore): AccountsCRUDRepository {
        return DefaultAccountsCRUDRepository(
            runtimeStore = RuntimeSharedStore(),
            userWalletsStore = userWalletsStore,
        )
    }
}