package com.tangem.data.networks.di

import com.tangem.data.networks.multi.DefaultMultiNetworkStatusProducer
import com.tangem.data.networks.single.DefaultSingleNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkStatusProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleNetworkStatusProducerFactory(
        impl: DefaultSingleNetworkStatusProducer.Factory,
    ): SingleNetworkStatusProducer.Factory

    @Binds
    @Singleton
    fun bindMultiNetworkStatusProducerFactory(
        impl: DefaultMultiNetworkStatusProducer.Factory,
    ): MultiNetworkStatusProducer.Factory
}