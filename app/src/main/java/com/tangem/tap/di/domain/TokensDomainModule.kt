package com.tangem.tap.di.domain

import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object TokensDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetTokenListUseCase(
        tokensRepository: TokensRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(tokensRepository, quotesRepository, networksRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListGroupingUseCase(
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCase {
        return ToggleTokenListGroupingUseCase(networksRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListSortingUseCase(dispatchers: CoroutineDispatcherProvider): ToggleTokenListSortingUseCase {
        return ToggleTokenListSortingUseCase(dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideApplyTokenListSortingUseCase(
        tokensRepository: TokensRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(tokensRepository, dispatchers)
    }
}