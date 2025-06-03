package com.tangem.data.staking.di

import com.tangem.data.staking.multi.DefaultMultiYieldBalanceProducer
import com.tangem.data.staking.single.DefaultSingleYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldBalanceProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleYieldBalanceProducerFactory(
        impl: DefaultSingleYieldBalanceProducer.Factory,
    ): SingleYieldBalanceProducer.Factory

    @Binds
    @Singleton
    fun bindMultiYieldBalanceProducerFactory(
        impl: DefaultMultiYieldBalanceProducer.Factory,
    ): MultiYieldBalanceProducer.Factory
}