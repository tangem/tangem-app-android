package com.tangem.data.quotes.di

import com.tangem.data.quotes.multi.DefaultMultiQuoteFetcher
import com.tangem.data.quotes.multi.DefaultMultiQuoteUpdater
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.multi.MultiQuoteUpdater
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface QuoteFetcherModule {

    @Binds
    @Singleton
    fun bindMultiQuoteFetcher(impl: DefaultMultiQuoteFetcher): MultiQuoteFetcher

    @Binds
    @Singleton
    fun bindMultiQuoteUpdater(impl: DefaultMultiQuoteUpdater): MultiQuoteUpdater
}