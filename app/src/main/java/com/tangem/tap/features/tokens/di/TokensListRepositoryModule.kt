package com.tangem.tap.features.tokens.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.tap.features.tokens.data.DefaultTokensListRepository
import com.tangem.tap.features.tokens.domain.TokensListRepository
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
* [REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TokensListRepositoryModule {

    @Provides
    @Singleton
    fun providesTokensListRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
        reduxStateHolder: AppStateHolder,
        testnetTokensStorage: TestnetTokensStorage,
    ): TokensListRepository {
        return DefaultTokensListRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
            reduxStateHolder = reduxStateHolder,
            testnetTokensStorage = testnetTokensStorage,
        )
    }
}
