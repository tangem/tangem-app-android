package com.tangem.features.onboarding.v2.twin.impl.di

import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.features.onboarding.v2.twin.impl.issuers.DefaultIssuersConfigStorage
import com.tangem.features.onboarding.v2.twin.impl.issuers.IssuersConfigStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object IssuersModule {

    @Provides
    @Singleton
    fun provideIssuersConfigStorage(assetLoader: AssetLoader): IssuersConfigStorage {
        return DefaultIssuersConfigStorage(
            assetLoader = assetLoader,
            runtimeStateStore = RuntimeStateStore(defaultValue = null),
        )
    }
}