package com.tangem.tap.features.tokens.impl.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.features.tokens.impl.data.DefaultTokensListRepository
import com.tangem.tap.features.tokens.impl.domain.TokensListRepository
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
        getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        testnetTokensStorage: TestnetTokensStorage,
    ): TokensListRepository {
        return DefaultTokensListRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            testnetTokensStorage = testnetTokensStorage,
        )
    }
}
