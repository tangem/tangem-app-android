package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.datastore.SharedPreferencesDataStore
import com.tangem.datasource.local.quote.DefaultQuotesStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.local.quote.model.StoredQuote
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuotesStoreModule {

    @Provides
    @Singleton
    fun provideQuotesStore(@ApplicationContext context: Context, @NetworkMoshi moshi: Moshi): QuotesStore {
        return DefaultQuotesStore(
            dataStore = SharedPreferencesDataStore(
                preferencesName = "quotes",
                context = context,
                adapter = moshi.adapter(StoredQuote::class.java),
            ),
        )
    }
}