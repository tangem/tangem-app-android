package com.tangem.tap.features.tokens.impl.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.testnet.TestnetTokensStorage
import com.tangem.tap.features.tokens.impl.data.DefaultTokensListRepository
import com.tangem.tap.features.tokens.impl.domain.DefaultTokensListInteractor
import com.tangem.tap.features.tokens.impl.domain.TokensListInteractor
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * @author Andrew Khokhlov on 12/04/2023
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
