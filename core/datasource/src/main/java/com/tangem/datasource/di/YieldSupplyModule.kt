package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import com.tangem.datasource.local.yieldsupply.DefaultYieldMarketsStore
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
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
object YieldSupplyModule {

    @Provides
    @Singleton
    fun provideYieldMarketsStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldMarketsStore {
        return DefaultYieldMarketsStore(
            persistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<YieldSupplyMarketTokenDto>(),
                    defaultValue = emptyList(),
                ),
                produceFile = { context.dataStoreFile(fileName = "yield_markets_cache") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}