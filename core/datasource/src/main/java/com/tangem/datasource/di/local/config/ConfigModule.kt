package com.tangem.datasource.di.local.config

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.config.environment.DefaultEnvironmentConfigStorage
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.config.issuers.DefaultIssuersConfigStorage
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.datasource.local.config.providers.BlockchainProvidersStorage
import com.tangem.datasource.local.config.providers.DefaultBlockchainProvidersStorage
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
    fun provideTestnetTokensStorage(assetLoader: AssetLoader): TestnetTokensStorage {
        return DefaultTestnetTokensStorage(assetLoader)
    }

    @Provides
    @Singleton
    fun provideProvidersOrderConfigStorage(assetLoader: AssetLoader): BlockchainProvidersStorage {
        return DefaultBlockchainProvidersStorage(
            assetLoader = assetLoader,
            runtimeStateStore = RuntimeStateStore(defaultValue = emptyMap()),
        )
    }

    @Provides
    @Singleton
    fun provideIssuersConfigStorage(assetLoader: AssetLoader): IssuersConfigStorage {
        return DefaultIssuersConfigStorage(
            assetLoader = assetLoader,
            runtimeStateStore = RuntimeStateStore(defaultValue = emptyList()),
        )
    }
}