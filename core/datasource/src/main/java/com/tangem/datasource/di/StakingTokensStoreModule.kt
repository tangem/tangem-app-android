package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultStakingTokensStore
import com.tangem.datasource.local.token.StakingTokensStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingTokensStoreModule {

    @Provides
    @Singleton
    fun provideStakingTokensStore(): StakingTokensStore {
        return DefaultStakingTokensStore(dataStore = RuntimeDataStore())
    }
}