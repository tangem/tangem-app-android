package com.tangem.data.yield.supply.di

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.yield.supply.store.DefaultYieldBoostPromoStore
import com.tangem.data.yield.supply.store.DefaultYieldBoostStatusStore
import com.tangem.data.yield.supply.store.YieldBoostPromoStore
import com.tangem.data.yield.supply.store.YieldBoostStatusStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldSupplyModule {

    @Provides
    @Singleton
    fun provideYieldBoostPromoStore(): YieldBoostPromoStore {
        return DefaultYieldBoostPromoStore(dataStore = RuntimeSharedStore())
    }

    @Provides
    @Singleton
    fun provideYieldBoostStatusStore(): YieldBoostStatusStore {
        return DefaultYieldBoostStatusStore(dataStore = RuntimeSharedStore())
    }
}