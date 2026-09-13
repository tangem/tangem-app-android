package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.grow.datasource.express.models.response.Asset
import com.tangem.datasource.local.token.DefaultExpressAssetsStore
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExpressAssetsStoreModule {

    @Provides
    @Singleton
    fun provideExpressAssetsStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
        dataStoreFactory: AppDataStoreFactory,
    ): ExpressAssetsStore {
        return DefaultExpressAssetsStore(
            persistenceStore = dataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithStringKeyTypes(listTypes<Asset>()),
                    defaultValue = emptyMap(),
                ),
                produceFile = { context.dataStoreFile("express_assets") },
                scope = appScope,
            ),
            runtimeStore = RuntimeSharedMapStore(),
        )
    }
}