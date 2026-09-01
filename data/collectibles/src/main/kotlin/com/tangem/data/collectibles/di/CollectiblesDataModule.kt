package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.DefaultCollectiblesRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CollectiblesDataModule {

    @Provides
    @Singleton
    fun provideCollectiblesRepository(
        collectiblesApi: CollectiblesApi,
        dispatchers: CoroutineDispatcherProvider,
    ): CollectiblesRepository {
        return DefaultCollectiblesRepository(collectiblesApi = collectiblesApi, dispatchers = dispatchers)
    }
}