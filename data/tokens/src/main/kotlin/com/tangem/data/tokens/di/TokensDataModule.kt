package com.tangem.data.tokens.di

import com.tangem.data.tokens.repository.MockNetworksRepository
import com.tangem.data.tokens.repository.MockQuotesRepository
import com.tangem.data.tokens.repository.MockTokensRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.tokens.repository.TokensRepository
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
    fun provideTokensRepository(): TokensRepository = MockTokensRepository()

    @Provides
    @Singleton
    fun provideQuotesRepository(): QuotesRepository = MockQuotesRepository()

    @Provides
    @Singleton
    fun provideNetworksRepository(): NetworksRepository = MockNetworksRepository()
}
