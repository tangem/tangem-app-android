package com.tangem.datasource.di

import com.tangem.datasource.asset.reader.AndroidAssetReader
import com.tangem.datasource.asset.reader.AssetReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AssetModule {

    @Binds
    @Singleton
    fun bindAsserReader(androidAssetReader: AndroidAssetReader): AssetReader
}