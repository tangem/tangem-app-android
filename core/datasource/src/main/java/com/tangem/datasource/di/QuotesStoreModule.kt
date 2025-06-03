package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.quote.DefaultQuotesStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
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
    fun providePersistenceQuotesStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): DataStore<Map<String, QuotesResponse.Quote>> {
        return DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes<QuotesResponse.Quote>(),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "quotes") },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideQuotesStore(persistenceStore: DataStore<Map<String, QuotesResponse.Quote>>): QuotesStore {
        return DefaultQuotesStore(
            persistenceStore = persistenceStore,
            runtimeStore = RuntimeSharedStore(),
        )
    }
}