package com.tangem.data.networks.di

import com.tangem.data.networks.multi.DefaultMultiNetworkStatusFetcher
import com.tangem.data.networks.single.DefaultSingleNetworkStatusFetcher
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkStatusFetcherModule {

    @Binds
    @Singleton
    fun bindSingleNetworkStatusFetcher(impl: DefaultSingleNetworkStatusFetcher): SingleNetworkStatusFetcher

    @Binds
    @Singleton
    fun bindMultiNetworkStatusFetcher(impl: DefaultMultiNetworkStatusFetcher): MultiNetworkStatusFetcher
}