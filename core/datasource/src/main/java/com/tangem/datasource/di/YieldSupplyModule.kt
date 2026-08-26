package com.tangem.datasource.di

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.yieldsupply.promo.DefaultYieldBoostPromoStore
import com.tangem.datasource.local.yieldsupply.promo.DefaultYieldBoostStatusStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostPromoStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostStatusStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YieldSupplyModule {

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