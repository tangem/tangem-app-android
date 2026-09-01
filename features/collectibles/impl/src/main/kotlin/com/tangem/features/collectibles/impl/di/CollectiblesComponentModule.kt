package com.tangem.features.collectibles.impl.di

import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import com.tangem.features.collectibles.impl.DefaultCollectiblesEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesComponentModule {

    @Binds
    @Singleton
    fun bindCollectiblesEntryComponentFactory(
        impl: DefaultCollectiblesEntryComponent.Factory,
    ): CollectiblesEntryComponent.Factory
}