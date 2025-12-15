package com.tangem.data.staking.di

import com.tangem.data.staking.multi.DefaultMultiStakingBalanceFetcher
import com.tangem.data.staking.single.DefaultSingleStakingBalanceFetcher
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingBalanceFetcherModule {

    @Binds
    @Singleton
    fun bindSingleStakingBalanceFetcher(impl: DefaultSingleStakingBalanceFetcher): SingleStakingBalanceFetcher

    @Binds
    @Singleton
    fun bindMultiStakingBalanceFetcher(impl: DefaultMultiStakingBalanceFetcher): MultiStakingBalanceFetcher
}