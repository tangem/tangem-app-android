package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.visa.DefaultTangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.DefaultTangemPayCloseCardStore
import com.tangem.datasource.local.visa.DefaultTangemPayIssueCardStore
import com.tangem.datasource.local.visa.DefaultTangemPayReissueCardStore
import com.tangem.datasource.local.visa.TangemPayCardFrozenStateStore
import com.tangem.datasource.local.visa.TangemPayCloseCardStore
import com.tangem.datasource.local.visa.TangemPayIssueCardStore
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
    fun provideTangemPayCardFrozenStateStore(): TangemPayCardFrozenStateStore {
        return DefaultTangemPayCardFrozenStateStore(
            dataStore = RuntimeDataStore(),
        )
    }

    @Provides
    @Singleton
    fun provideTangemPayReissueCardStore(prefs: AppPreferencesStore): TangemPayReissueCardStore {
        return DefaultTangemPayReissueCardStore(
            feeStore = RuntimeDataStore(),
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