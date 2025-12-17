package com.tangem.data.staking.di

import com.tangem.data.staking.multi.DefaultMultiStakingBalanceProducer
import com.tangem.data.staking.single.DefaultSingleStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingBalanceProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleStakingBalanceProducerFactory(
        impl: DefaultSingleStakingBalanceProducer.Factory,
    ): SingleStakingBalanceProducer.Factory

    @Binds
    @Singleton
    fun bindMultiStakingBalanceProducerFactory(
        impl: DefaultMultiStakingBalanceProducer.Factory,
    ): MultiStakingBalanceProducer.Factory
}