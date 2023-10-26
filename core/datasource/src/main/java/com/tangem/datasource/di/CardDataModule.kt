package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.local.card.DefaultUsedCardsStore
import com.tangem.datasource.local.card.UsedCardInfo
import com.tangem.datasource.local.card.UsedCardsStore
import com.tangem.datasource.local.datastore.JsonSharedPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDataModule {

    @Provides
    @Singleton
    fun provideUsedCardsStore(@ApplicationContext context: Context, @NetworkMoshi moshi: Moshi): UsedCardsStore {
        return DefaultUsedCardsStore(
            store = JsonSharedPreferencesDataStore(
                preferencesName = "tapPrefs",
                context = context,
                adapter = moshi.adapter(
                    Types.newParameterizedType(List::class.java, UsedCardInfo::class.java),
                ),
            ),
        )
    }
}