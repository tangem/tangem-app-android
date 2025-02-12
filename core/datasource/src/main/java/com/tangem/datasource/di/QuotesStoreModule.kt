package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.quote.DefaultQuotesStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.local.quote.utils.QuotesSerializer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuotesStoreModule {

    @Provides
    @Singleton
    fun provideQuotesStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): QuotesStore {
        return DefaultQuotesStore(
            dataStore = DataStoreFactory.create(
                serializer = QuotesSerializer(moshi),
                produceFile = { context.dataStoreFile(fileName = "quotes") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}