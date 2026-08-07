package com.tangem.spend.datasource.pay.store

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
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
    fun provideTangemPayCardFrozenStateStore(): TangemPayCardFrozenStateStore {
        return DefaultTangemPayCardFrozenStateStore(
            store = RuntimeSharedMapStore(),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayReissueCardStore(prefs: AppPreferencesStore): TangemPayReissueCardStore {
        return DefaultTangemPayReissueCardStore(
            feeStore = RuntimeSharedMapStore(),
            prefs = prefs,
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayCloseCardStore(prefs: AppPreferencesStore): TangemPayCloseCardStore {
        return DefaultTangemPayCloseCardStore(
            prefs = prefs,
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayIssueCardStore(prefs: AppPreferencesStore): TangemPayIssueCardStore {
        return DefaultTangemPayIssueCardStore(
            prefs = prefs,
        )
    }
}