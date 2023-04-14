package com.tangem.tap.features.tokens.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.tap.features.tokens.data.DefaultTokensListRepository
import com.tangem.tap.features.tokens.domain.DefaultTokensListInteractor
import com.tangem.tap.features.tokens.domain.TokensListInteractor
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
* [REDACTED_AUTHOR]
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object TokensListInteractorModule {

    @Provides
    @ViewModelScoped
    fun provideTokensListInteractor(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
        reduxStateHolder: AppStateHolder,
        testnetTokensStorage: TestnetTokensStorage,
    ): TokensListInteractor {
        return DefaultTokensListInteractor(
            repository = DefaultTokensListRepository(
                tangemTechApi = tangemTechApi,
                dispatchers = dispatchers,
                reduxStateHolder = reduxStateHolder,
                testnetTokensStorage = testnetTokensStorage,
            ),
            reduxStateHolder = reduxStateHolder,
        )
    }
}
