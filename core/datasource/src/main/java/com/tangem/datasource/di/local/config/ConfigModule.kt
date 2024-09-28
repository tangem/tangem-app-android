package com.tangem.datasource.di.local.config

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.config.environment.DefaultEnvironmentConfigStorage
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.testnet.DefaultTestnetTokensStorage
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
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
    fun provideEnvironmentConfigStorage(assetLoader: AssetLoader): EnvironmentConfigStorage {
        return DefaultEnvironmentConfigStorage(
            assetLoader = assetLoader,
            environmentConfigStore = RuntimeStateStore(defaultValue = EnvironmentConfig()),
        )
    }

    @Provides
    @Singleton
    fun providesTestnetTokensStorage(assetLoader: AssetLoader): TestnetTokensStorage {
        return DefaultTestnetTokensStorage(assetLoader)
    }
}
