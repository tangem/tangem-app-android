package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.delegate.DefaultCollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesDelegateModule {

    @Binds
    fun bindCollectiblesHotDelegate(impl: DefaultCollectiblesHotDelegate): CollectiblesHotDelegate
}