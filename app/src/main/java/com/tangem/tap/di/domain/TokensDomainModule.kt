package com.tangem.tap.di.domain

import com.tangem.domain.tokens.GetTokenListUseCase
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
    ) = GetTokenListUseCase(tokensRepository, quotesRepository, networksRepository, dispatchers)
}
