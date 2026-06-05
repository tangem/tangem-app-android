package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.visa.DefaultTangemPayPendingOrdersStore
import com.tangem.datasource.local.visa.DefaultTangemPayReissueCardStore
import com.tangem.datasource.local.visa.TangemPayPendingOrdersStore
import com.tangem.datasource.local.visa.TangemPayReissueCardStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemPayStoresModule {

    @Provides
    @Singleton
    fun provideTangemPayReissueCardStore(): TangemPayReissueCardStore {
        return DefaultTangemPayReissueCardStore(
            feeStore = RuntimeDataStore(),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayPendingOrdersStore(prefs: AppPreferencesStore): TangemPayPendingOrdersStore {
        return DefaultTangemPayPendingOrdersStore(prefs = prefs)
    }
}