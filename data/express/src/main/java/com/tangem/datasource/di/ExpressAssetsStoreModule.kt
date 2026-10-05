package com.tangem.datasource.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.grow.datasource.express.models.response.Asset
import com.tangem.datasource.local.token.DefaultExpressAssetsStore
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ExpressAssetsStoreModule {

    @Provides
    @Singleton
    fun provideExpressAssetsStore(dataStoreFactory: AppDataStoreFactory): ExpressAssetsStore {
        return DefaultExpressAssetsStore(
            persistenceStore = dataStoreFactory.create(
                defaultValue = emptyMap<String, List<Asset>>(),
                fileName = "express_assets",
            ),
            runtimeStore = RuntimeSharedMapStore(),
        )
    }
}