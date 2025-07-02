package com.tangem.data.quotes.di

import com.tangem.data.quotes.multi.DefaultMultiQuoteStatusFetcher
import com.tangem.data.quotes.single.DefaultSingleQuoteStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface QuoteStatusFetcherModule {

    @Binds
    @Singleton
    fun bindMultiQuoteStatusFetcher(impl: DefaultMultiQuoteStatusFetcher): MultiQuoteStatusFetcher

    @Binds
    @Singleton
    fun bindSingleQuoteStatusFetcher(impl: DefaultSingleQuoteStatusFetcher): SingleQuoteStatusFetcher
}