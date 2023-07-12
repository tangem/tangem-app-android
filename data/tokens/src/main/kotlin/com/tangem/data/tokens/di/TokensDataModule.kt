package com.tangem.data.tokens.di

import com.tangem.data.tokens.repository.DefaultNetworksRepository
import com.tangem.data.tokens.repository.DefaultQuotesRepository
import com.tangem.data.tokens.repository.DefaultTokensRepository
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
    fun provideTokensRepository(): TokensRepository = DefaultTokensRepository()

    @Provides
    @Singleton
    fun provideQuotesRepository(): QuotesRepository = DefaultQuotesRepository()

    @Provides
    @Singleton
    fun provideNetworksRepository(): NetworksRepository = DefaultNetworksRepository()
}
