package com.tangem.data.collectibles.di

import com.tangem.data.collectibles.DefaultCollectiblesRepository
import com.tangem.data.collectibles.derivation.DefaultCollectiblesOwnerDeriver
import com.tangem.domain.collectibles.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
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
    fun provideCollectiblesOwnerDeriver(
        userWalletsListRepository: UserWalletsListRepository,
        derivationsRepository: DerivationsRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): CollectiblesOwnerDeriver {
        return DefaultCollectiblesOwnerDeriver(
            userWalletsListRepository = userWalletsListRepository,
            derivationsRepository = derivationsRepository,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideCollectiblesRepository(
        collectiblesApi: CollectiblesApi,
        dispatchers: CoroutineDispatcherProvider,
    ): CollectiblesRepository {
        return DefaultCollectiblesRepository(collectiblesApi = collectiblesApi, dispatchers = dispatchers)
    }
}