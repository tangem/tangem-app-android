package com.tangem.data.account.di

import com.tangem.data.account.fetcher.DefaultMultiAccountListFetcher
import com.tangem.data.account.fetcher.DefaultSingleAccountListFetcher
import com.tangem.domain.account.fetcher.MultiAccountListFetcher
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountListFetcherModule {

    @Binds
    @Singleton
    fun bindSingleAccountListFetcher(impl: DefaultSingleAccountListFetcher): SingleAccountListFetcher

    @Binds
    @Singleton
    fun bindMultiAccountListFetcher(impl: DefaultMultiAccountListFetcher): MultiAccountListFetcher
}