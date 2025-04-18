package com.tangem.data.quotes.di

import com.tangem.data.quotes.DefaultQuotesRepositoryV2
import com.tangem.domain.quotes.QuotesRepositoryV2
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface QuotesDataModule {

    @Binds
    @Singleton
    fun bindQuotesRepositoryV2(impl: DefaultQuotesRepositoryV2): QuotesRepositoryV2
}