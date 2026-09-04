package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.DefaultCollectiblesRepository
import com.tangem.data.collectibles.derivation.DefaultCollectiblesOwnerDeriver
import com.tangem.domain.collectibles.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesDataModule {

    @Binds
    @Singleton
    fun bindCollectiblesOwnerDeriver(impl: DefaultCollectiblesOwnerDeriver): CollectiblesOwnerDeriver

    @Binds
    @Singleton
    fun bindCollectiblesRepository(impl: DefaultCollectiblesRepository): CollectiblesRepository
}