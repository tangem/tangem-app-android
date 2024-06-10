package com.tangem.datasource.di

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.testnet.DefaultTestnetTokensStorage
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TestnetTokensStorageModule {

    @Provides
    @Singleton
    fun providesTestnetTokensStorage(assetLoader: AssetLoader): TestnetTokensStorage {
        return DefaultTestnetTokensStorage(assetLoader)
    }
}