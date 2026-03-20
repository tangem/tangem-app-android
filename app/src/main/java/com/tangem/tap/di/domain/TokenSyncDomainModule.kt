package com.tangem.tap.di.domain

import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.tokensync.repository.TokenSyncRepository
import com.tangem.domain.tokensync.usecase.AcknowledgeTokenSyncCompletionUseCase
import com.tangem.domain.tokensync.usecase.ObserveTokenSyncUseCase
import com.tangem.domain.tokensync.usecase.SyncTokensUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenSyncDomainModule {

    @Provides
    @Singleton
    fun provideObserveTokenSyncUseCase(tokenSyncRepository: TokenSyncRepository): ObserveTokenSyncUseCase {
        return ObserveTokenSyncUseCase(
            tokenSyncRepository = tokenSyncRepository,
        )
    }

    @Provides
    @Singleton
    fun provideAcknowledgeTokenSyncCompletionUseCase(
        tokenSyncRepository: TokenSyncRepository,
    ): AcknowledgeTokenSyncCompletionUseCase {
        return AcknowledgeTokenSyncCompletionUseCase(
            tokenSyncRepository = tokenSyncRepository,
        )
    }

    @Provides
    @Singleton
    fun provideSyncTokensUseCase(
        tokenSyncRepository: TokenSyncRepository,
        manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
        appCoroutineScope: AppCoroutineScope,
    ): SyncTokensUseCase {
        return SyncTokensUseCase(
            tokenSyncRepository = tokenSyncRepository,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            appCoroutineScope = appCoroutineScope,
        )
    }
}