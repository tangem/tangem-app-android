package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.local.testnet.DefaultTestnetTokensStorage
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author Andrew Khokhlov on 08/04/2023
 */
@Module
@InstallIn(SingletonComponent::class)
object TestnetTokensStorageModule {

    @OptIn(ExperimentalStdlibApi::class)
    @Provides
    @Singleton
    fun providesTestnetTokensStorage(assetReader: AssetReader, @SdkMoshi moshi: Moshi): TestnetTokensStorage {
        return DefaultTestnetTokensStorage(assetReader = assetReader, adapter = moshi.adapter())
    }
}
