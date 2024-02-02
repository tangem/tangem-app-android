package com.tangem.core.deeplink.di

import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.impl.DefaultDeepLinksRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DeepLinksModule {

    @Provides
    @Singleton
    fun provideDeepLinksRegistry(): DeepLinksRegistry {
        return DefaultDeepLinksRegistry()
    }
}