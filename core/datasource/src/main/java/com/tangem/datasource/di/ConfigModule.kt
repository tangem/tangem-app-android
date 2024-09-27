package com.tangem.datasource.di

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.DefaultConfigManager
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.local.datastore.RuntimeStateStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ConfigModule {

    @Provides
    @Singleton
    fun providesConfigManager(assetLoader: AssetLoader): ConfigManager {
        return DefaultConfigManager(
            assetLoader = assetLoader,
            configStore = RuntimeStateStore(defaultValue = Config()),
        )
    }
}