package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.*
import com.tangem.datasource.local.token.DefaultStakingBalanceStore
import com.tangem.datasource.local.token.DefaultStakingYieldsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingStoreModule {

    @Provides
    @Singleton
    fun provideStakingTokensStore(): StakingYieldsStore {
        return DefaultStakingYieldsStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideStakingBalanceStore(): StakingBalanceStore {
        return DefaultStakingBalanceStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideStakingActionsStore(): StakingActionsStore {
        return DefaultStakingActionsStore(dataStore = RuntimeDataStore())
    }
}