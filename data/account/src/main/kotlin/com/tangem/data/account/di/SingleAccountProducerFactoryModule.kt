package com.tangem.data.account.di

import com.tangem.data.account.producer.DefaultSingleAccountProducer
import com.tangem.domain.account.producer.SingleAccountProducer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SingleAccountProducerFactoryModule {

    @Binds
    @Singleton
    fun bindSingleAccountProducerFactory(impl: DefaultSingleAccountProducer.Factory): SingleAccountProducer.Factory
}