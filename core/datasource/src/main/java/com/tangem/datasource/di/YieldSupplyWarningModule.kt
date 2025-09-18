package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.token.DefaultYieldSupplyWarningActionStore
import com.tangem.datasource.local.token.YieldSupplyWarningActionStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.setTypes
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
object YieldSupplyWarningModule {

    @Provides
    @Singleton
    fun provideYieldSupplyWarningStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldSupplyWarningActionStore {
        return DefaultYieldSupplyWarningActionStore(
            persistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = setTypes<String>(),
                    defaultValue = emptySet(),
                ),
                produceFile = { context.dataStoreFile(fileName = "yield_supply_warnings_viewed") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            dispatchers = dispatchers,
        )
    }
}