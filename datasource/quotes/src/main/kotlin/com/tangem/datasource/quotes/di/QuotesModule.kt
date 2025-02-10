package com.tangem.datasource.quotes.di

import com.tangem.datasource.quotes.DefaultQuotesDataSource
import com.tangem.datasource.quotes.QuotesDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface QuotesModule {

    @Binds
    fun bindQuotesDataSource(impl: DefaultQuotesDataSource): QuotesDataSource
}