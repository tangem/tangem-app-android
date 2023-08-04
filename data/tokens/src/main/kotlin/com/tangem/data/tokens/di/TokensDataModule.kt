package com.tangem.data.tokens.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.repository.DefaultTokensRepository
import com.tangem.data.tokens.repository.MockNetworksRepository
import com.tangem.data.tokens.repository.MockQuotesRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokensDataModule {

    @Provides
    @Singleton
    fun provideTokensRepository(
        tangemTechApi: TangemTechApi,
        userTokensStore: UserTokensStore,
        userWalletsStore: UserWalletsStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
    ): TokensRepository {
        return DefaultTokensRepository(tangemTechApi, userTokensStore, userWalletsStore, cacheRegistry, dispatchers)
    }

    @Provides
    @Singleton
    fun provideQuotesRepository(): QuotesRepository = MockQuotesRepository()

    @Provides
    @Singleton
    fun provideNetworksRepository(): NetworksRepository = MockNetworksRepository()
}