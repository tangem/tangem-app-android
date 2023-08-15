package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
import com.tangem.datasource.local.appcurrency.implementation.DefaultAvailableAppCurrenciesStore
import com.tangem.datasource.local.appcurrency.implementation.DefaultSelectedAppCurrencyStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.datastore.SharedPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppCurrencyDataModule {

    @Provides
    @Singleton
    fun provideAvailableAppCurrenciesStore(): AvailableAppCurrenciesStore {
        return DefaultAvailableAppCurrenciesStore(
            dataStore = RuntimeDataStore(),
        )
    }

    @Provides
    @Singleton
    fun provideSelectedAppCurrencyStore(
        @ApplicationContext context: Context,
        @NetworkMoshi moshi: Moshi,
    ): SelectedAppCurrencyStore {
        return DefaultSelectedAppCurrencyStore(
            dataStore = SharedPreferencesDataStore(
                preferencesName = "selected_app_currency",
                context = context,
                adapter = moshi.adapter(CurrenciesResponse.Currency::class.java),
            ),
        )
    }
}