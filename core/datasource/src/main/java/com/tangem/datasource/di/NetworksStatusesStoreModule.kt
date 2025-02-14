package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.DefaultNetworksStatusesStore
import com.tangem.datasource.local.network.NetworksStatusesStore
import com.tangem.datasource.local.network.utils.NetworkStatusesSerializer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal object NetworksStatusesStoreModule {

    @Provides
    fun provideNetworksStatusesStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): NetworksStatusesStore {
        return DefaultNetworksStatusesStore(
            runtimeDataStore = RuntimeDataStore(),
            persistenceDataStore = DataStoreFactory.create(
                serializer = NetworkStatusesSerializer(moshi),
                produceFile = { context.dataStoreFile(fileName = "networks_statuses") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}
