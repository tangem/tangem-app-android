package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultExpressAssetsStore
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
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
internal object ExpressAssetsStoreModule {

    @Provides
    @Singleton
    fun provideExpressAssetsStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): ExpressAssetsStore {
        return DefaultExpressAssetsStore(
            persistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithStringKeyTypes(listTypes<Asset>()),
                    defaultValue = emptyMap(),
                ),
                produceFile = { context.dataStoreFile("express_assets") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            runtimeStore = RuntimeDataStore(),
        )
    }
}