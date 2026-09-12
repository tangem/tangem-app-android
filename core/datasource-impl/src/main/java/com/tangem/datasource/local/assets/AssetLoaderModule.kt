package com.tangem.datasource.local.assets

import com.tangem.core.local.assets.AssetLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AssetLoaderModule {

    @Binds
    fun bindAssetLoader(impl: DefaultAssetLoader): AssetLoader
}