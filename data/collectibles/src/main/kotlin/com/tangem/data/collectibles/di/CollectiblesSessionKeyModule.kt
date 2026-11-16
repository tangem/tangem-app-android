package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.delegate.DefaultCollectiblesSessionKey
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesSessionKeyModule {

    @Binds
    fun bindCollectiblesSessionKey(impl: DefaultCollectiblesSessionKey): CollectiblesSessionKey
}