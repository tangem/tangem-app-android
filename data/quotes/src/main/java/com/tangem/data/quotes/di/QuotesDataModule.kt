package com.tangem.data.quotes.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.quotes.repository.DefaultQuotesRepository
import com.tangem.data.quotes.store.DefaultQuotesStatusesStore
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.domain.quotes.QuotesRepository
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
internal object QuotesDataModule {

    @Singleton
    @Provides
    fun provideQuotesStoreV2(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): QuotesStatusesStore {
        return DefaultQuotesStatusesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithStringKeyTypes<QuotesResponse.Quote>(),
                    defaultValue = emptyMap(),
                ),
                produceFile = { context.dataStoreFile(fileName = "quotes") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            dispatchers = dispatchers,
        )
    }

    @Singleton
    @Provides
    fun providesQuotesRepository(impl: DefaultQuotesRepository): QuotesRepository = impl
}