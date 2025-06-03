package com.tangem.data.staking.di

import com.tangem.data.staking.multi.DefaultMultiYieldBalanceFetcher
import com.tangem.data.staking.single.DefaultSingleYieldBalanceFetcher
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldBalanceFetcherModule {

    @Binds
    @Singleton
    fun bindSingleYieldBalanceFetcher(impl: DefaultSingleYieldBalanceFetcher): SingleYieldBalanceFetcher

    @Binds
    @Singleton
    fun bindMultiYieldBalanceFetcher(impl: DefaultMultiYieldBalanceFetcher): MultiYieldBalanceFetcher
}