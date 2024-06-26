package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultStakingBalanceStore
import com.tangem.datasource.local.token.StakingBalanceStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingBalanceStoreModule {

    @Provides
    @Singleton
    fun provideStakingBalanceStore(): StakingBalanceStore {
        return DefaultStakingBalanceStore(dataStore = RuntimeDataStore())
    }
}
